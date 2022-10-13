import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

class KlibDiffTaskConfig(
    val binary: JsIrBinary,
    val klibDiffExtension: KlibDiffExtension
    ) {
    val taskConfigActions = mutableListOf<(TaskProvider<KlibDiffTask>) -> Unit>()

    private var executed = false

    init {
        configureTask { task ->
            task.outputDir.set(binary.compilation.project.layout.buildDirectory.dir("klib-diff"))
            task.libraries.from({ binary.compilation.compileDependencyFiles })
            task.threshold.value(klibDiffExtension.threshold).finalizeValue()
        }
    }

    fun configureTaskProvider(configAction: (TaskProvider<KlibDiffTask>) -> Unit) {
        check(!executed) {
            "Task has already been configured. Configuration actions should be added to this object before `this.execute` method runs."
        }
        taskConfigActions.add(configAction)
    }

    fun configureTask(configAction: (KlibDiffTask) -> Unit) {
        configureTaskProvider { taskProvider ->
            taskProvider.configure(configAction)
        }
    }

    fun execute(taskProvider: TaskProvider<KlibDiffTask>) {
        executed = true
        taskConfigActions.forEach {
            it(taskProvider)
        }
    }
}