import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File
import javax.inject.Inject

abstract class KlibDiffTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Incremental
    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val threshold: Property<Int>

    @TaskAction
    fun calculateDiff(inputChanges: InputChanges) {
        println(inputChanges.isIncremental)
        val addedFiles = mutableListOf<File>()
        val modifiedFiles = mutableListOf<File>()
        val removedFiles = mutableListOf<File>()
        inputChanges.getFileChanges(libraries)
            .forEach { change ->
                when (change.changeType) {
                    ChangeType.ADDED -> addedFiles.add(change.file)
                    ChangeType.MODIFIED -> modifiedFiles.add(change.file)
                    ChangeType.REMOVED -> removedFiles.add(change.file)
                }
            }
        println("ADDED")
        addedFiles.forEach {
            println(it)
        }

        println("MOD")
        modifiedFiles.forEach {
            println(it)
        }

        println("REMOVED")
        removedFiles.forEach {
            println(it)
        }

        KlibPatchMaker(
            outputDir.asFile.get(),
            libraries.files,
            execOperations,
            threshold.get()
        )
    }
}