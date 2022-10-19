package klib.director

import java.io.File
import java.security.MessageDigest
import kotlin.math.max

class KlibRecorder(
    private val mainModuleKlib: File,
    private val addedKlibs: List<File>,
    private val modifiedKlibs: List<File>,
    private val removedKlibs: List<File>,
    private val outputDirectory: File,
    private val klibsLimit: Int
) {
    private val latestDir = outputDirectory.latestGenerationDir()
    private val latestFilesTxt = latestDir.filesTxt()

    private val md5 = MessageDigest.getInstance("MD5")

    private val generations: KlibGenerations by lazy { KlibGenerations(outputDirectory) }

    private fun File.uniqInLatestDir(): File {
        if (this == mainModuleKlib) {
            return latestDir.mainKlib()
        }
        val h = md5.digest(absolutePath.encodeToByteArray()).joinToString(separator = "") { b -> "%02x".format(b) }
        return latestDir.resolve("$h.$name")
    }

    private fun removeOutdatedKlibs() {
        val generationsToRemove = max((generations.last - generations.first) - klibsLimit + 1, 0)
        for (generation in generations.first until generations.first + generationsToRemove) {
            outputDirectory.resolve("$generation").deleteRecursively()
        }
    }

    private fun createAndFillLatestDir() {
        latestDir.mkdirs()
        val klibs = KlibFiles()
        // TODO check if we have only added files?
        for (klib in addedKlibs + modifiedKlibs) {
            val fileInLatestDir = klib.uniqInLatestDir()
            klib.copyTo(fileInLatestDir)
            klibs[fileInLatestDir.name] = klib.absolutePath
        }
        klibs.saveTo(latestFilesTxt)
    }

    fun storeModifiedKlibs() {
        if (!latestDir.exists()) {
            createAndFillLatestDir()
            return
        }

        val klibs = KlibFiles(latestFilesTxt)

        val prevGenerationDir = outputDirectory.resolve("${generations.last}")
        prevGenerationDir.mkdirs()
        latestFilesTxt.moveTo(prevGenerationDir.filesTxt())

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
            fileInLatestDir.moveTo(prevGenerationDir.resolve(fileInLatestDir.name))
            klib.copyTo(fileInLatestDir, true)
        }

        klibs.saveTo(latestFilesTxt)

        removeOutdatedKlibs()
    }
}
