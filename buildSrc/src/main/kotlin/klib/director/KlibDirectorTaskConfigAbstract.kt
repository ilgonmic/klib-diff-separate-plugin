package klib.director

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskProvider

abstract class KlibDirectorTaskConfigAbstract<T: DefaultTask> {
    val taskConfigActions = mutableListOf<(TaskProvider<T>) -> Unit>()

    private var executed = false

    abstract fun configActionImpl(task: T)

    init {
        configureTaskProvider { taskProvider ->
            taskProvider.configure(::configActionImpl)
        }
    }

    fun configureTaskProvider(configAction: (TaskProvider<T>) -> Unit) {
        check(!executed) {
            "Task has already been configured. Configuration actions should be added to this object before `this.execute` method runs."
        }
        taskConfigActions.add(configAction)
    }

    fun execute(taskProvider: TaskProvider<T>) {
        executed = true
        taskConfigActions.forEach {
            it(taskProvider)
        }
    }
}
