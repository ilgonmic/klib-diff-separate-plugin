import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.ChangeType
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import java.io.File

abstract class KlibDiffTask : DefaultTask() {

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Incremental
    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @TaskAction
    fun calculateDiff(inputChanges: InputChanges) {
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
    }
}