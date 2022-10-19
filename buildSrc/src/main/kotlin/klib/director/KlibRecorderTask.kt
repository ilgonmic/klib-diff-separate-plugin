package klib.director

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.process.ExecOperations
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import java.io.File

abstract class KlibRecorderTask : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Incremental
    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Incremental
    abstract val entryModule: RegularFileProperty

    @get:Input
    abstract val klibsLimit: Property<Int>

    @TaskAction
    fun storeModifiedKlibs(inputChanges: InputChanges) {
        val addedFiles = mutableListOf<File>()
        val modifiedFiles = mutableListOf<File>()
        val removedFiles = mutableListOf<File>()
        val modules = inputChanges.getFileChanges(libraries) + inputChanges.getFileChanges(entryModule)
        modules.forEach { change ->
            if (change.file.isKlibFile() && change.file.exists()) {
                when (change.changeType) {
                    ChangeType.ADDED -> addedFiles.add(change.file)
                    ChangeType.MODIFIED -> modifiedFiles.add(change.file)
                    ChangeType.REMOVED -> removedFiles.add(change.file)
                }
            }
        }

        KlibRecorder(
            entryModule.get().asFile,
            addedFiles,
            modifiedFiles,
            removedFiles,
            outputDir.asFile.get(),
            klibsLimit.get()
        ).storeModifiedKlibs()
    }
}

class KlibRecorderTaskConfig(private val binary: JsIrBinary, private val klibsLimit: Int) :
    KlibDirectorTaskConfigAbstract<KlibRecorderTask>() {
    override fun configActionImpl(task: KlibRecorderTask) {
        val project = binary.compilation.project
        task.outputDir.set(project.layout.buildDirectory.dir("klib-records"))
        task.libraries.from({ binary.compilation.compileDependencyFiles })
        task.klibsLimit.value(klibsLimit).finalizeValue()
        task.entryModule.set(
            project.tasks.named(binary.compilation.target.artifactsTaskName, Jar::class.java)
                .flatMap { it.archiveFile }
        )
    }
}
