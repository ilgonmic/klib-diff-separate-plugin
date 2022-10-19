package klib.director

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

class KlibDirectorPlugin : Plugin<Project> {
    private enum class Properties(val prop: String) {
        KLIBS_LIMIT("klib_director.records_limit"),
        PLAY_FROM_DIR("klib_director.play_from_dir"),
        CONFIGURATION_CACHE("org.gradle.unsafe.configuration-cache")
    }

    private fun String.asTaskName(jsBinary: JsIrBinary): String {
        return this + jsBinary.name.capitalize()
    }

    private fun registerKlibRecorderTask(jsBinary: JsIrBinary, project: Project, klibsLimit: Int) {
        val taskProvider = project.tasks.register("recordKlib".asTaskName(jsBinary), KlibRecorderTask::class.java)
        KlibRecorderTaskConfig(jsBinary, klibsLimit).execute(taskProvider)
        jsBinary.linkTask.configure {
            dependsOn(taskProvider)
        }
    }

    private fun registerKlibPlayerTask(jsBinary: JsIrBinary, project: Project, playFromDir: String) {
        val taskProvider = project.tasks.register("playKlib".asTaskName(jsBinary), KlibPlayerTask::class.java)
        KlibPlayerTaskConfig(jsBinary, playFromDir).execute(taskProvider)
        jsBinary.linkTask.configure {
            dependsOn(taskProvider)
            libraries.setFrom({ taskProvider.map { it.patchedLibraries } })
        }

        project.rootProject.allprojects {
            this.tasks.withType(Kotlin2JsCompile::class.java).matching { it !is KotlinJsIrLink }.all {
                onlyIf { false }
            }
        }
    }

    private fun registerJsICCleanerTask(jsBinary: JsIrBinary, project: Project) {
        val taskProvider = project.tasks.register("cleanJsIC".asTaskName(jsBinary), JsICCleanerTask::class.java)
        JsICCleanerTaskConfig(jsBinary).execute(taskProvider)
    }

    override fun apply(project: Project) {
        val klibsLimit = (project.findProperty(Properties.KLIBS_LIMIT.prop) as? String)?.toIntOrNull()
        val playFromDir = project.findProperty(Properties.PLAY_FROM_DIR.prop) as? String
        if (klibsLimit == null && playFromDir == null) {
            return
        }
        check(klibsLimit == null || playFromDir == null) {
            "'${Properties.KLIBS_LIMIT.prop}' and '${Properties.PLAY_FROM_DIR.prop}' are incompatible, please choose one of them"
        }

        val configurationCacheEnabled = project.findProperty(Properties.CONFIGURATION_CACHE.prop) == "true"
        check(playFromDir == null || !configurationCacheEnabled) {
            "'${Properties.PLAY_FROM_DIR.prop}' can not be used with enabled '${Properties.CONFIGURATION_CACHE.prop}'"
        }

        project.plugins.withType(KotlinBasePlugin::class.java) {
            val kotlinExtension = project.kotlinExtension
            val action: (KotlinJsTargetDsl) -> Unit = { target ->
                target
                    .binaries
                    .matching { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
                    .all {
                        if (klibsLimit != null) {
                            registerKlibRecorderTask(this as JsIrBinary, project, klibsLimit)
                            registerJsICCleanerTask(this, project)
                        }

                        if (playFromDir != null) {
                            registerKlibPlayerTask(this as JsIrBinary, project, playFromDir)
                        }
                    }
            }
            when (kotlinExtension) {
                is KotlinJsProjectExtension -> kotlinExtension.registerTargetObserver {
                    action(it!!)
                }

                is KotlinMultiplatformExtension -> kotlinExtension
                    .targets
                    .matching { it is KotlinJsTargetDsl }
                    .configureEach {
                        this as KotlinJsTargetDsl
                        action(this)
                    }
            }
        }
    }
}
