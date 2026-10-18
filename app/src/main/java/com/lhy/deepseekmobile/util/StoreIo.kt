package com.lhy.deepseekmobile.util

import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

val AppJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

val CompactJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Write via a sibling temp file so a kill mid-write never leaves a half JSON behind. */
fun File.writeTextAtomic(text: String) {
    parentFile?.mkdirs()
    val tmp = File(parentFile, "$name.tmp")
    FileOutputStream(tmp).use { out ->
        out.write(text.toByteArray(StandardCharsets.UTF_8))
        out.fd.sync()
    }
    if (exists()) delete()
    if (!tmp.renameTo(this)) {
        tmp.copyTo(this, overwrite = true)
        tmp.delete()
    }
}

fun File.readTextOrNull(): String? = runCatching { if (exists()) readText(Charsets.UTF_8) else null }.getOrNull()
