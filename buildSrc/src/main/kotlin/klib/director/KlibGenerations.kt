package klib.director

import java.io.File
import java.nio.file.Files
import kotlin.streams.toList

class KlibGenerations(patchesDir: File) {
    private val allGenerations = if (patchesDir.exists()) {
        Files.walk(patchesDir.toPath(), 1).map {
            it.fileName.toString()
        }.toList().mapNotNull { it.toIntOrNull() }
    } else {
        emptyList()
    }

    val last = (allGenerations.maxOrNull() ?: -1) + 1
    val first = allGenerations.minOrNull() ?: 0
}
