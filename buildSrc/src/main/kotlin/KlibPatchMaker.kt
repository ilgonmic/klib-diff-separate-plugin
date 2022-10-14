import org.gradle.process.ExecOperations
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.streams.toList

class KlibPatchMaker(
    private val addedKlibs: List<File>,
    private val modifiedKlibs: List<File>,
    private val removedKlibs: List<File>,
    private val outputDirectory: File,
    private val execOperations: ExecOperations,
    private val historyLimit: Int
) {
    private val filesTxt = "files.txt"
    private val latestDir = outputDirectory.resolve("latest")
    private val latestFilesTxt = latestDir.resolve(filesTxt)

    private val md5 = MessageDigest.getInstance("MD5")

    private val allGenerations: List<Int> by lazy {
        if (outputDirectory.exists()) {
            Files.walk(outputDirectory.toPath(), 1).map {
                it.fileName.toString()
            }.toList().mapNotNull { it.toIntOrNull() }
        } else {
            emptyList()
        }
    }

    private val lastGeneration: Int by lazy { (allGenerations.maxOrNull() ?: -1) + 1 }

    private val firstGeneration: Int by lazy { allGenerations.minOrNull() ?: 0 }

    private fun File.uniqInLatestDir(): File {
        val h = md5.digest(absolutePath.encodeToByteArray()).joinToString(separator = "") { b -> "%02x".format(b) }
        return latestDir.resolve("$h.$name")
    }

    private fun removeOutdatedPatches() {
        val generationsToRemove = max((lastGeneration - firstGeneration) - historyLimit + 1, 0)
        for (generation in firstGeneration until firstGeneration + generationsToRemove) {
            outputDirectory.resolve("$generation").deleteRecursively()
        }
    }

    private fun File.moveTo(other: File) {
        if (!renameTo(other)) {
            copyTo(other, true)
            delete()
        }
    }

    private fun createAndFillLatestDir() {
        latestDir.mkdirs()
        val klibs = hashMapOf<String, String>()
        // TODO check if we have only added files?
        for (klib in addedKlibs + modifiedKlibs) {
            val fileInLatestDir = klib.uniqInLatestDir()
            klib.copyTo(fileInLatestDir)
            klibs[fileInLatestDir.name] = klib.absolutePath
        }
        klibs.saveToLatestFilesTxt()
    }

    fun Map<String, String>.saveToLatestFilesTxt() {
        latestFilesTxt.writeText(entries.joinToString(separator = "\n") {
            "${it.key}/${it.value}"
        })
    }

    fun saveKLibPatches() {
        println("Saving klibs patches...")
        if (!latestDir.exists()) {
            createAndFillLatestDir()
            return
        }

        val klibs = latestFilesTxt.readLines().associateTo(hashMapOf()) {
            val sep = it.indexOfFirst { c -> c == '/' }
            it.substring(0, sep) to it.substring(sep + 1)
        }

        val prevGenerationDir = outputDirectory.resolve("$lastGeneration")
        prevGenerationDir.mkdirs()
        latestFilesTxt.moveTo(prevGenerationDir.resolve(filesTxt))

        for (klib in removedKlibs) {
            val fileInLatestDir = klib.uniqInLatestDir()
            fileInLatestDir.moveTo(prevGenerationDir.resolve(fileInLatestDir.name))
            klibs.remove(fileInLatestDir.name)
        }

        for (klib in addedKlibs) {
            val fileInLatestDir = klib.uniqInLatestDir()
            klib.copyTo(fileInLatestDir)
            klibs[fileInLatestDir.name] = klib.absolutePath
        }

        for (klib in modifiedKlibs) {
            val fileInLatestDir = klib.uniqInLatestDir()
            val patchFile = prevGenerationDir.resolve("${fileInLatestDir.name}.bspatch")
            execOperations.exec {
                workingDir = outputDirectory
                executable = "bsdiff"
                // Usage: bsdiff OLD_FILE NEW_FILE OUT_PATCH_FILE
                // Intentionally make a patch from new klib -> old klib
                // So to restore we need to apply the patches to stored klib file
                args = listOf(klib.absolutePath, fileInLatestDir.absolutePath, patchFile.absolutePath)
            }
            klib.copyTo(fileInLatestDir, true)
        }

        klibs.saveToLatestFilesTxt()

        removeOutdatedPatches()
    }
}
