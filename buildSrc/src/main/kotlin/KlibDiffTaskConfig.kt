import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary

class KlibDiffTaskConfig(
    val binary: JsIrBinary,
    val klibDiffExtension: KlibDiffExtension
    ) {
    val taskConfigActions = mutableListOf<(TaskProvider<KlibDiffTask>) -> Unit>()

    private var executed = false

    init {
        configureTask { task ->
            val project = binary.compilation.project
            task.outputDir.set(project.layout.buildDirectory.dir("klib-diff"))
            task.libraries.from({ binary.compilation.compileDependencyFiles })
            task.historyLimit.value(klibDiffExtension.historyLimit).finalizeValue()
            task.entryModule.set(
                project.tasks.named(binary.compilation.target.artifactsTaskName, Jar::class.java)
                    .flatMap { it.archiveFile }
            )
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
