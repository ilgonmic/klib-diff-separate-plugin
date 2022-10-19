package klib.director

import java.io.File

class KlibFiles private constructor(private val files: MutableMap<String, String>) :
    MutableMap<String, String> by files {

    constructor() : this(hashMapOf())

    constructor(f: File) : this(hashMapOf()) {
        f.readLines().associateTo(this) {
            val sep = it.indexOfFirst { c -> c == '/' }
            it.substring(0, sep) to it.substring(sep + 1)
        }
    }

    fun saveTo(f: File) {
        f.writeText(entries.joinToString(separator = "\n") {
            "${it.key}/${it.value}"
        })
    }
}
