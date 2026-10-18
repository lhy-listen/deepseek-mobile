package com.lhy.deepseekmobile.ingest

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class FileText(
    val name: String,
    val path: String,
    val text: String,
    val truncated: Boolean,
    val bytes: Long,
)

data class FolderText(
    val rootName: String,
    val files: List<FileText>,
    val skipped: Int,
) {
    val totalChars: Int get() = files.sumOf { it.text.length }

    fun toPrompt(): String = buildString {
        append("【文件夹：").append(rootName).append("】\n")
        append("已读取 ").append(files.size).append(" 个文本文件")
        if (skipped > 0) append("，跳过 ").append(skipped).append(" 个二进制/超大文件")
        append("。\n\n目录：\n")
        files.take(200).forEach { append("- ").append(it.path).append('\n') }
        append("\n———— 文件内容 ————\n")
        files.forEach { f ->
            append("\n<<<").append(f.path).append(">>>\n")
            append(f.text)
            if (f.truncated) append("\n…（该文件较长，已截断）")
            append("\n<<<END ").append(f.path).append(">>>\n")
        }
    }
}

/**
 * Turns SAF documents / whole folders into a compact, readable context block.
 * Text detection is heuristic (NUL bytes + printable ratio) so a stray binary
 * never poisons the prompt.
 */
class TextIngestor(private val context: Context) {

    private val documents = DocumentExtractor(context)

    suspend fun readFile(uri: Uri): Result<FileText> = withContext(Dispatchers.IO) {
        runCatching {
            val meta = queryMeta(uri)
            if (documents.supports(meta.first)) {
                val text = documents.extract(uri, meta.first)
                    ?: error("无法从 ${meta.first} 提取文字（可能是扫描件）")
                return@runCatching FileText(
                    name = meta.first,
                    path = meta.first,
                    text = text.take(MAX_FILE_CHARS),
                    truncated = text.length > MAX_FILE_CHARS,
                    bytes = meta.second,
                )
            }
            require(meta.second <= MAX_FILE_BYTES) { "文件超过 ${MAX_FILE_BYTES / 1024 / 1024}MB，已跳过" }
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("无法读取文件")
            require(isProbablyText(bytes)) { "看起来是二进制文件，已跳过" }
            val text = decode(bytes)
            FileText(
                name = meta.first,
                path = meta.first,
                text = text.take(MAX_FILE_CHARS),
                truncated = text.length > MAX_FILE_CHARS,
                bytes = meta.second,
            )
        }
    }

    suspend fun readTree(uri: Uri, maxFiles: Int = 180): Result<FolderText> = withContext(Dispatchers.IO) {
        runCatching {
            val root = DocumentFile.fromTreeUri(context, uri) ?: error("无法打开文件夹")
            val files = mutableListOf<FileText>()
            var skipped = 0
            var budget = MAX_TOTAL_CHARS

            fun walk(dir: DocumentFile, prefix: String, depth: Int) {
                if (depth > 6 || files.size >= maxFiles || budget <= 0) return
                val children = dir.listFiles().sortedBy { it.name ?: "" }
                for (child in children) {
                    if (files.size >= maxFiles || budget <= 0) return
                    val name = child.name.orEmpty()
                    if (name.startsWith(".")) continue
                    if (child.isDirectory) {
                        if (name.lowercase() in SKIP_DIRS) continue
                        walk(child, if (prefix.isEmpty()) name else "$prefix/$name", depth + 1)
                    } else {
                        if (!isTextName(name, child.type) && !documents.supports(name)) { skipped++; continue }
                        val size = child.length()
                        if (size <= 0 || (size > MAX_FILE_BYTES && !documents.supports(name))) { skipped++; continue }
                        val decoded = if (documents.supports(name)) {
                            documents.extract(child.uri, name)?.take(MAX_FILE_CHARS)?.take(budget)
                        } else {
                            val bytes = runCatching {
                                context.contentResolver.openInputStream(child.uri)?.use { it.readBytes() }
                            }.getOrNull()
                            if (bytes == null || !isProbablyText(bytes)) { skipped++; continue }
                            decode(bytes).take(MAX_FILE_CHARS).take(budget)
                        }
                        if (decoded.isNullOrBlank()) { skipped++; continue }
                        budget -= decoded.length
                        val path = if (prefix.isEmpty()) name else "$prefix/$name"
                        files += FileText(name, path, decoded, decoded.length >= MAX_FILE_CHARS, size)
                    }
                }
            }

            walk(root, "", 0)
            require(files.isNotEmpty()) { "这个文件夹里没有可读取的文本文件" }
            FolderText(root.name ?: "所选文件夹", files, skipped)
        }
    }

    private fun queryMeta(uri: Uri): Pair<String, Long> {
        var name = "未命名"
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        name = c.getString(0) ?: name
                        size = if (c.isNull(1)) 0L else c.getLong(1)
                    }
                }
        }
        return name to size
    }

    private fun decode(bytes: ByteArray): String {
        val trimmed = if (bytes.size > 4 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte()) bytes.copyOfRange(3, bytes.size) else bytes
        return runCatching {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(trimmed))
                .toString()
        }.getOrElse {
            runCatching { String(trimmed, Charset.forName("GB18030")) }.getOrDefault(String(trimmed, Charsets.UTF_8))
        }
    }

    private fun isProbablyText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sample = bytes.copyOfRange(0, minOf(bytes.size, 4096))
        if (sample.any { it == 0.toByte() }) return false
        var printable = 0
        var control = 0
        sample.forEach { b ->
            val v = b.toInt() and 0xFF
            when {
                v == 9 || v == 10 || v == 13 -> printable++
                v < 32 -> control++
                else -> printable++
            }
        }
        val ratio = printable.toFloat() / sample.size
        return ratio > 0.92f && control.toFloat() / sample.size < 0.02f
    }

    private fun isTextName(name: String, mime: String?): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext in TEXT_EXTENSIONS) return true
        return mime?.startsWith("text/") == true
    }

    companion object {
        private const val MAX_FILE_BYTES = 1_500_000L
        private const val MAX_FILE_CHARS = 120_000
        private const val MAX_TOTAL_CHARS = 520_000

        private val SKIP_DIRS = setOf(
            "node_modules", "build", "dist", ".git", ".idea", ".gradle", "__pycache__",
            "venv", ".venv", "target", "out", ".next", "vendor", "pods",
        )

        private val TEXT_EXTENSIONS = setOf(
            "txt", "md", "markdown", "rst", "org", "csv", "tsv", "json", "jsonl",
            "yaml", "yml", "xml", "html", "htm", "css", "scss", "js", "mjs", "ts", "tsx", "jsx",
            "kt", "kts", "java", "py", "c", "h", "cpp", "hpp", "cc", "cs", "go", "rs", "rb", "php",
            "swift", "m", "mm", "sh", "bash", "zsh", "ps1", "bat", "sql", "ini", "toml", "cfg",
            "conf", "properties", "gradle", "tex", "bib", "log", "gitignore", "editorconfig", "env",
        )
    }
}
