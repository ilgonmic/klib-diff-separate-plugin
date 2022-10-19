package klib.director

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import java.io.File
import javax.inject.Inject

abstract class KlibPlayerTask : DefaultTask() {
    @get:Internal
    abstract val workingDir: DirectoryProperty

    @get:Internal
    abstract val incrementalCacheDir: Property<File>

    @get:OutputFiles
    var patchedLibraries = emptySet<File>()

    @get:NormalizeLineEndings
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    internal abstract val entryModule: DirectoryProperty

    @get:Incremental
    @get:Classpath
    abstract val libraries: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val playFromDir: Property<String>

    @TaskAction
    fun fetchKlibs(inputChanges: InputChanges) {
        val workingDir = workingDir.asFile.get()
        val klibsDir = File(playFromDir.get())
        if (!klibsDir.exists() || !klibsDir.isDirectory) {
            println("Can't find klib play directory $klibsDir")
            workingDir.deleteInernals()
            patchedLibraries = libraries.files
            return
        }

        try {
            val player = KlibPlayer(
                libraries.files,
                workingDir,
                klibsDir,
                incrementalCacheDir.get(),
                entryModule.get().asFile
            )
            patchedLibraries = player.fetchKlibs()
        } catch (e: Exception) {
            println("Can't apply patches from directry $klibsDir: ${e.message ?: "unknown error"}")
            patchedLibraries = libraries.files
        }
    }
}

class KlibPlayerTaskConfig(private val binary: JsIrBinary, private val playFromDir: String) :
    KlibDirectorTaskConfigAbstract<KlibPlayerTask>() {
    override fun configActionImpl(task: KlibPlayerTask) {
        val project = binary.compilation.project
        task.workingDir.set(project.layout.buildDirectory.dir("klib-player"))
        task.libraries.from({ binary.compilation.compileDependencyFiles })
        task.playFromDir.value(playFromDir).finalizeValue()
        task.outputs.upToDateWhen { false }
        task.incrementalCacheDir.set(binary.linkTask.map { it.rootCacheDirectory })
        task.entryModule.fileProvider(
            binary.compilation.output.classesDirs.elements.flatMap {
                task.project.providers.provider {
                    it.single().asFile.also { entryDir -> entryDir.mkdirs() }
                }
            }
        )

        task.dependsOn(binary.compilation.compileTaskProvider)
    }
}
