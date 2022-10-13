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
        project.plugins.withType(KotlinBasePlugin::class.java) {
            val kotlinExtension = project.kotlinExtension
            val action: (KotlinJsTargetDsl) -> Unit = { target ->
                target
                    .binaries
                    .matching { it.mode == KotlinJsBinaryMode.DEVELOPMENT }
                    .all {
                        project.tasks.register(this.name + "KlibDiff", KlibDiffTask::class.java).also {
                            KlibDiffTaskConfig(this as JsIrBinary).execute(it)
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