package klib.director

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import java.io.File

abstract class JsICCleanerTask : DefaultTask() {
    @get:Internal
    abstract val incrementalCacheDir: Property<File>

    @TaskAction
    fun clearIncrementalCache(inputChanges: InputChanges) {
        incrementalCacheDir.get().deleteRecursively()
    }
}

class JsICCleanerTaskConfig(private val binary: JsIrBinary) :
    KlibDirectorTaskConfigAbstract<JsICCleanerTask>() {
    override fun configActionImpl(task: JsICCleanerTask) {
        task.outputs.upToDateWhen { false }
        task.incrementalCacheDir.set(binary.linkTask.map { it.rootCacheDirectory })
    }
}
