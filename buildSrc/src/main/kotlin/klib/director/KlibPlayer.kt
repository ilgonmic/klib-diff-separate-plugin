package klib.director

import java.io.File
import kotlin.math.min

class KlibPlayer(
    private val klibs: Set<File>,
    private val workingDir: File,
    private val klibsDir: File,
    private val incrementalCacheDir: File,
    private val mainModuleDir: File
) {
    private val generations = KlibGenerations(klibsDir)
    private val generationTxt = workingDir.resolve("generation.txt")
    private val currentDir = workingDir.resolve("current")


    private fun readNextGeneration(): Int {
        if (!generationTxt.exists()) {
            return generations.first
        }
        val lines = generationTxt.readLines()
        if (lines.size < 2) {
            return generations.first
        }
        val prevGeneration = lines[0].toIntOrNull() ?: return generations.first
        if (lines[1] != klibsDir.absolutePath) {
            return generations.first
        }
        return min(prevGeneration + 1, generations.last)
    }

    private fun saveGeneration(generation: Int) {
        generationTxt.writeText("$generation\n${klibsDir.absolutePath}")
    }

    private fun getGenerationDir(generation: Int): File {
        if (generation == generations.last) {
            return klibsDir.latestGenerationDir()
        }
        return klibsDir.resolve(generation.toString())
    }

    fun fetchKlib(klib: File, generation: Int) {
        check(generation <= generations.last) { "Can't restore file ${klib.name}" }
        val generationDir = getGenerationDir(generation)

        val generationKlib = generationDir.resolve(klib.name)
        if (generationKlib.exists()) {
            generationKlib.copyTo(klib)
        } else {
            fetchKlib(klib, generation + 1)
        }
    }

    fun fetchKlibs(): Set<File> {
        currentDir.deleteRecursively()
        currentDir.mkdirs()

        val generation = readNextGeneration()
        if (generation == generations.first) {
            incrementalCacheDir.deleteRecursively()
        }

        val klibs = KlibFiles(getGenerationDir(generation).filesTxt())
        val fetchedKlibs = hashSetOf<File>()
        val mainKlib = currentDir.mainKlib()
        for (klib in klibs.keys) {
            val klibFile = currentDir.resolve(klib)
            fetchKlib(klibFile, generation)
            if (klibFile == mainKlib) {
                klibFile.unzipTo(mainModuleDir)
            } else {
                fetchedKlibs += klibFile
            }
        }

        saveGeneration(generation)

        return fetchedKlibs
    }
}
