package com.lhy.deepseekmobile.ingest

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.util.zip.ZipFile

/**
 * Extracts plain text from PDF / DOCX / PPTX. PDF goes through PDFBox-Android;
 * OOXML formats are zip files, so we read their document XML directly and strip
 * tags — far lighter than pulling in a full office parser.
 */
class DocumentExtractor(private val context: Context) {

    fun supports(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in EXTENSIONS

    fun extract(uri: Uri, name: String): String? {
        val ext = name.substringAfterLast('.', "").lowercase()
        return runCatching {
            when (ext) {
                "pdf" -> extractPdf(uri)
                "docx" -> extractDocx(uri)
                "pptx" -> extractPptx(uri)
                else -> null
            }
        }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractPdf(uri: Uri): String? {
        val temp = File.createTempFile("ingest-", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            PDDocument.load(temp).use { document ->
                return PDFTextStripper().getText(document)
            }
        } finally {
            runCatching { temp.delete() }
        }
    }

    private fun extractDocx(uri: Uri): String? = withZip(uri) { zip ->
        val entry = zip.getEntry("word/document.xml") ?: return@withZip null
        val xml = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
        xml
            .replace(Regex("</w:p>"), "\n")
            .replace(Regex("<w:tab[^>]*/>"), "\t")
            .replace(Regex("<w:br[^>]*/>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .decodeXmlEntities()
    }

    private fun extractPptx(uri: Uri): String? = withZip(uri) { zip ->
        val slides = zip.entries().toList()
            .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
            .sortedBy { it.name.substringAfter("slide").substringBefore(".").toIntOrNull() ?: 0 }
        if (slides.isEmpty()) return@withZip null
        buildString {
            slides.forEachIndexed { index, entry ->
                val xml = zip.getInputStream(entry).readBytes().toString(Charsets.UTF_8)
                append("\n—— 第 ${index + 1} 页 ——\n")
                append(
                    xml.replace(Regex("</a:p>"), "\n")
                        .replace(Regex("<[^>]+>"), "")
                        .decodeXmlEntities()
                )
            }
        }
    }

    private fun <T> withZip(uri: Uri, block: (ZipFile) -> T?): T? {
        val temp = File.createTempFile("ingest-", ".zip", context.cacheDir)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            ZipFile(temp).use { zip -> block(zip) }
        } finally {
            runCatching { temp.delete() }
        }
    }

    private fun String.decodeXmlEntities(): String = this
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#10;", "\n")
        .replace("&amp;", "&")

    companion object {
        private val EXTENSIONS = setOf("pdf", "docx", "pptx")
    }
}
