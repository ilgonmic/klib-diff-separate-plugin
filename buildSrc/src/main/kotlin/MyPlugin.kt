import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

class MyPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val klibDiffExtension = project.extensions.create(KlibDiffExtension.NAME, KlibDiffExtension::class.java).also {
            it.threshold.convention(0)
        }

        project.plugins.withType(KotlinBasePlugin::class.java) {
            val kotlinExtension = project.kotlinExtension
            val action: (KotlinJsTargetDsl) -> Unit = { target ->
                target
                    .binaries
                    .matching { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
                    .all {
                        project.tasks.register(this.name + "KlibDiff", KlibDiffTask::class.java).also { taskProvider ->
                            KlibDiffTaskConfig(this as JsIrBinary, klibDiffExtension).execute(taskProvider)
                            this.linkTask.configure {
                                dependsOn(taskProvider)
                            }
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