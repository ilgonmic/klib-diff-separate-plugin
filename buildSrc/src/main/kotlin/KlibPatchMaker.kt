import org.gradle.process.ExecOperations
import java.io.BufferedInputStream
import java.io.File

class KlibPatchMaker(
    private val outputDirectory: File,
    private val libraries: Set<File>,
    private val execOperations: ExecOperations,
    private val threshold: Int
) {
    private var currentGeneration: Int = -1

    private val patchExtension: String
        get() {
            if (currentGeneration == -1) {
                val genFile = outputDirectory.resolve("_generation")
                if (genFile.exists()) {
                    currentGeneration = genFile.readText().toInt() + 1
                } else {
                    currentGeneration = 1
                }
                genFile.writeText(currentGeneration.toString())
            }
            return "${currentGeneration}.bspatch"
        }

    private val bspatchRegex = Regex("^.+\\.(\\d+)\\.bspatch$")

    private fun isKLibFilesEqual(firstFile: File, secondFile: File): Boolean {
        if (firstFile.length() != secondFile.length()) {
            return false
        }

        BufferedInputStream(firstFile.inputStream()).use { bf1 ->
            BufferedInputStream(secondFile.inputStream()).use { bf2 ->
                while (true) {
                    val c = bf1.read()
                    when {
                        c == -1 -> return true
                        c != bf2.read() -> return false
                    }
                }
            }
        }
    }

    private fun removeOutdatedPatches() {
        if (currentGeneration <= threshold) {
            return
        }

        for (diffFile in outputDirectory.walk()) {
            val m = bspatchRegex.matchEntire(diffFile.path) ?: continue
            val fileGeneration = m.groups[1]?.value?.toInt() ?: 0
            if (currentGeneration - fileGeneration >= threshold) {
                diffFile.delete()
            }
        }
    }

    fun saveKLibPatches() {
        println("Saving klibs patches...")
        outputDirectory.mkdirs()

        for (newKLibFile in libraries) {
            if (!newKLibFile.path.endsWith(".klib")) {
                continue
            }
            val prevKLibFile = outputDirectory.resolve(newKLibFile.name)
            if (prevKLibFile.exists()) {
                if (isKLibFilesEqual(prevKLibFile, newKLibFile)) {
                    continue
                }
                val diffFile = outputDirectory.resolve("${newKLibFile.name}.${patchExtension}")
                execOperations.exec {
                    workingDir = outputDirectory
                    executable = "bsdiff"
                    // Usage: bsdiff OLD_FILE NEW_FILE OUT_PATCH_FILE
                    // Intentionally make a patch newKLibFile -> prevKLibFile
                    // So to restore we need to apply the patches to stored klib file
                    args = listOf(newKLibFile.absolutePath, prevKLibFile.absolutePath, diffFile.absolutePath)
                }
            }
            newKLibFile.copyTo(prevKLibFile, true)
        }

        removeOutdatedPatches()
    }
}
