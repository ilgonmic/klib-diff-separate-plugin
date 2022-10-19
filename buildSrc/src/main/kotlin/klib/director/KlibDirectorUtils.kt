package klib.director

import java.io.File
import java.util.zip.ZipFile

internal fun File.filesTxt() = resolve("files.txt")

internal fun File.latestGenerationDir() = resolve("latest")

internal fun File.mainKlib() = resolve("main.klib")

internal fun File.isKlibFile() = path.endsWith(".klib") || path.endsWith(".jar")

internal fun File.moveTo(other: File) {
    if (!renameTo(other)) {
        copyTo(other, true)
        delete()
    }
}

internal fun File.deleteInernals(): Boolean = walkBottomUp().fold(true, { res, it ->
    it == this || ((it.delete() || !it.exists()) && res)
})

internal fun File.unzipTo(outDir: File) {
    outDir.deleteRecursively()
    outDir.mkdirs()

    ZipFile(this).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            zip.getInputStream(entry).use { input ->
                val targetFile = outDir.resolve(entry.name)
                if (entry.isDirectory) {
                    targetFile.mkdirs()
                } else {
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
