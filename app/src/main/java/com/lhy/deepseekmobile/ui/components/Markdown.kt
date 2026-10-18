package com.lhy.deepseekmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhy.deepseekmobile.ui.theme.LocalDsPalette
import kotlinx.coroutines.delay

/* ------------------------------------------------------------------ blocks */

sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Code(val lang: String, val code: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class ListBlock(val items: List<MdListItem>) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

data class MdListItem(
    val indent: Int,
    val ordered: Boolean,
    val number: Int?,
    val text: String,
)

object MdParser {

    private val HEADING = Regex("^(#{1,6})\\s+(.*)$")
    private val RULE = Regex("^([-*_])\\s*(\\1\\s*){2,}$")
    private val LIST = Regex("^(\\s*)([-*+]|\\d{1,3}[.)])\\s+(.*)$")

    fun parse(source: String): List<MdBlock> {
        val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = ArrayList<MdBlock>()
        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) { i++; continue }

            val fence = when {
                trimmed.startsWith("```") -> "```"
                trimmed.startsWith("~~~") -> "~~~"
                else -> null
            }
            if (fence != null) {
                val lang = trimmed.removePrefix(fence).trim()
                val body = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith(fence)) {
                    body.append(lines[i]).append('\n')
                    i++
                }
                if (i < lines.size) i++
                blocks += MdBlock.Code(lang, body.toString().trimEnd('\n'))
                continue
            }

            val headingMatch = HEADING.matchEntire(trimmed)
            if (headingMatch != null) {
                blocks += MdBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2].trim())
                i++
                continue
            }

            if (RULE.matches(trimmed)) {
                blocks += MdBlock.Rule
                i++
                continue
            }

            if (trimmed.startsWith(">")) {
                val quote = ArrayList<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quote += lines[i].trim().removePrefix(">").removePrefix(" ").trimEnd()
                    i++
                }
                blocks += MdBlock.Quote(quote.joinToString("\n").trim())
                continue
            }

            if (trimmed.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
                val header = splitRow(trimmed)
                i += 2
                val rows = ArrayList<List<String>>()
                while (i < lines.size && lines[i].contains("|") && lines[i].isNotBlank()) {
                    rows += splitRow(lines[i])
                    i++
                }
                blocks += MdBlock.Table(header, rows)
                continue
            }

            val firstItem = LIST.matchEntire(raw)
            if (firstItem != null) {
                val items = ArrayList<MdListItem>()
                while (i < lines.size) {
                    val line = lines[i]
                    val match = LIST.matchEntire(line)
                    if (match != null) {
                        val marker = match.groupValues[2]
                        val ordered = marker.firstOrNull()?.isDigit() == true
                        items += MdListItem(
                            indent = match.groupValues[1].length / 2,
                            ordered = ordered,
                            number = if (ordered) marker.dropLast(1).toIntOrNull() else null,
                            text = match.groupValues[3].trim(),
                        )
                        i++
                    } else if (items.isNotEmpty() && line.startsWith("  ") && line.isNotBlank() &&
                        !line.trimStart().startsWith("```") && !line.trimStart().startsWith(">")
                    ) {
                        items[items.lastIndex] = items.last().copy(text = items.last().text + " " + line.trim())
                        i++
                    } else break
                }
                blocks += MdBlock.ListBlock(items)
                continue
            }

            val paragraph = StringBuilder()
            while (i < lines.size) {
                val line = lines[i]
                if (line.isBlank()) break
                val t = line.trim()
                if (t.startsWith("```") || t.startsWith("~~~") || t.startsWith(">") ||
                    LIST.matches(line) || HEADING.matches(t) || RULE.matches(t)
                ) break
                if (t.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) break
                if (paragraph.isNotEmpty()) paragraph.append('\n')
                paragraph.append(t)
                i++
            }
            if (paragraph.isNotEmpty()) blocks += MdBlock.Paragraph(paragraph.toString())
        }
        return blocks
    }

    private fun isTableSeparator(line: String): Boolean {
        val cells = splitRow(line)
        if (cells.isEmpty()) return false
        return cells.all { it.trim().matches(Regex("^:?-{2,}:?$")) }
    }

    private fun splitRow(line: String): List<String> {
        var s = line.trim()
        if (s.startsWith("|")) s = s.substring(1)
        if (s.endsWith("|")) s = s.dropLast(1)
        return s.split('|').map { it.trim() }
    }
}

/* ------------------------------------------------------------------ inline */

@Composable
private fun inlineString(text: String): AnnotatedString {
    val scheme = MaterialTheme.colorScheme
    val codeBg = scheme.surfaceContainerHigh
    val linkColor = scheme.primary
    return remember(text, codeBg, linkColor) {
        buildInline(text, scheme.onSurface, codeBg, linkColor, scheme.onSurfaceVariant)
    }
}

private fun buildInline(
    text: String,
    base: Color,
    codeBg: Color,
    linkColor: Color,
    muted: Color,
): AnnotatedString = buildAnnotatedString {
    var i = 0
    fun appendPlain(value: String) {
        if (value.isNotEmpty()) withStyle(SpanStyle(color = base)) { append(value) }
    }
    while (i < text.length) {
        val c = text[i]

        if (c == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = base)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }

        if (c == '*' && i + 1 < text.length && text[i + 1] == '*' || c == '_' && i + 1 < text.length && text[i + 1] == '_') {
            val token = text.substring(i, i + 2)
            val end = text.indexOf(token, i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = base)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }

        if (c == '~' && i + 1 < text.length && text[i + 1] == '~') {
            val end = text.indexOf("~~", i + 2)
            if (end > i + 2) {
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = muted)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }

        if ((c == '*' || c == '_') && i + 1 < text.length && !text[i + 1].isWhitespace()) {
            val end = text.indexOf(c, i + 1)
            if (end > i + 1 && text[end - 1] != ' ') {
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = base)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }

        if (c == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket > i && closeBracket + 1 < text.length && text[closeBracket + 1] == '(') {
                val closeParen = text.indexOf(')', closeBracket + 2)
                if (closeParen > closeBracket) {
                    val label = text.substring(i + 1, closeBracket)
                    val url = text.substring(closeBracket + 2, closeParen)
                    runCatching {
                        withLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(
                                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                                )
                            )
                        ) { append(label) }
                    }.onFailure {
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) { append(label) }
                    }
                    i = closeParen + 1
                    continue
                }
            }
        }

        if (c == 'h' && text.startsWith("http://", i) || c == 'h' && text.startsWith("https://", i)) {
            var end = i
            while (end < text.length && !text[end].isWhitespace()) end++
            val url = text.substring(i, end)
            runCatching {
                withLink(LinkAnnotation.Url(url, TextLinkStyles(style = SpanStyle(color = linkColor)))) { append(url) }
            }.onFailure {
                withStyle(SpanStyle(color = linkColor)) { append(url) }
            }
            i = end
            continue
        }

        appendPlain(c.toString())
        i++
    }
}

/* ---------------------------------------------------------------- rendering */

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val blocks = remember(text) { MdParser.parse(text) }
    Column(modifier) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    Text(
                        text = block.text,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineSmall
                            2 -> MaterialTheme.typography.titleLarge
                            3 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        },
                        color = color,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 10.dp, bottom = 4.dp),
                    )
                }

                is MdBlock.Paragraph -> {
                    Text(
                        text = inlineString(block.text),
                        style = style,
                        color = color,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }

                is MdBlock.Code -> CodeBlock(block.lang, block.code)

                is MdBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .heightIn(min = 20.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = inlineString(block.text),
                            style = style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is MdBlock.ListBlock -> {
                    Column(Modifier.padding(bottom = 2.dp)) {
                        block.items.forEach { item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = (item.indent * 14).dp, bottom = 3.dp)
                            ) {
                                Text(
                                    text = markerOf(item),
                                    style = style,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.widthIn(min = 20.dp),
                                )
                                Text(
                                    text = inlineString(item.text),
                                    style = style,
                                    color = color,
                                )
                            }
                        }
                    }
                }

                is MdBlock.Table -> TableBlock(block.header, block.rows, style)

                MdBlock.Rule -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

private fun markerOf(item: MdListItem): String {
    if (item.text.startsWith("[ ] ")) return "☐"
    if (item.text.startsWith("[x] ", ignoreCase = true)) return "☑"
    return when {
        item.ordered -> "${item.number ?: 1}."
        item.indent == 0 -> "•"
        else -> "◦"
    }
}

@Composable
private fun TableBlock(header: List<String>, rows: List<List<String>>, style: TextStyle) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            Modifier
                .clip(shape)
                .border(1.dp, scheme.outlineVariant, shape)
        ) {
            Row(Modifier.background(scheme.surfaceContainerHigh)) {
                header.forEach { cell ->
                    Text(
                        text = cell,
                        style = style.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = scheme.onSurface,
                        modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            rows.forEachIndexed { rowIndex, row ->
                HorizontalDivider(color = scheme.outlineVariant)
                Row {
                    row.forEachIndexed { cellIndex, cell ->
                        Text(
                            text = cell,
                            style = style.copy(fontSize = 13.sp),
                            color = if (cellIndex == 0) scheme.onSurface else scheme.onSurfaceVariant,
                            modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlock(lang: String, code: String) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDsPalette.current.dark
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val highlighted = remember(code, lang, dark) { CodeHighlighter.highlight(code, lang, dark) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1400)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.surfaceContainer)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainerHigh.copy(alpha = 0.6f))
                .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lang.ifBlank { "CODE" }.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (copied) scheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickableNoRipple {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                    contentDescription = "复制代码",
                    tint = if (copied) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = scheme.onSurface,
                softWrap = false,
            )
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = MutableInteractionSource(),
        indication = null,
        onClick = onClick,
    )

/* -------------------------------------------------------------- highlighter */

object CodeHighlighter {

    private data class Palette(
        val keyword: Color,
        val string: Color,
        val comment: Color,
        val number: Color,
        val function: Color,
        val type: Color,
        val plain: Color,
    )

    private val darkPalette = Palette(
        keyword = Color(0xFFC792EA),
        string = Color(0xFFA5E075),
        comment = Color(0xFF6E7A8E),
        number = Color(0xFFF78C6C),
        function = Color(0xFF82AAFF),
        type = Color(0xFFFFCB6B),
        plain = Color(0xFFD6DEEB),
    )

    private val lightPalette = Palette(
        keyword = Color(0xFF8B3BB5),
        string = Color(0xFF2E7D32),
        comment = Color(0xFF8A94A6),
        number = Color(0xFFB45309),
        function = Color(0xFF1D4ED8),
        type = Color(0xFF9A6700),
        plain = Color(0xFF1F2430),
    )

    private val commonKeywords = setOf(
        "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while",
        "return", "import", "package", "private", "public", "internal", "protected", "override",
        "suspend", "data", "sealed", "companion", "init", "try", "catch", "finally", "throw",
        "null", "true", "false", "is", "as", "in", "out", "by", "lateinit", "const", "enum",
        "typealias", "operator", "inline", "reified", "vararg", "this", "super", "do", "break",
        "continue", "where", "def", "lambda", "yield", "with", "pass", "raise", "except", "from",
        "global", "nonlocal", "assert", "del", "elif", "async", "await", "function", "let",
        "const", "new", "typeof", "instanceof", "delete", "void", "switch", "case", "default",
        "extends", "implements", "static", "final", "abstract", "synchronized", "volatile",
        "transient", "native", "strictfp", "throws", "this", "struct", "union", "sizeof",
        "typedef", "using", "namespace", "template", "typename", "virtual", "explicit", "friend",
        "auto", "extern", "register", "short", "long", "signed", "unsigned", "goto", "fn", "let",
        "mut", "pub", "impl", "trait", "match", "loop", "use", "crate", "mod", "where", "dyn",
        "package", "func", "defer", "go", "chan", "select", "map", "range", "type", "fallthrough",
    )

    private val typeKeywords = setOf(
        "String", "Int", "Long", "Double", "Float", "Boolean", "Char", "Byte", "Short", "Any",
        "Unit", "Nothing", "List", "Map", "Set", "Array", "IntArray", "ArrayList", "HashMap",
        "Integer", "Character", "Object", "Void", "Exception", "Error", "true", "false", "null",
        "None", "True", "False", "self", "string", "number", "boolean", "any", "unknown",
    )

    fun highlight(code: String, lang: String, dark: Boolean): AnnotatedString {
        val palette = if (dark) darkPalette else lightPalette
        val language = normalize(lang)
        val keywords = keywordsFor(language)
        val lineComment = when (language) {
            "python", "shell", "ruby", "yaml", "toml", "r", "dockerfile", "makefile" -> "#"
            "sql", "lua", "haskell" -> "--"
            "html", "xml", "css" -> null
            else -> "//"
        }
        return buildAnnotatedString {
            var i = 0
            while (i < code.length) {
                val c = code[i]

                if (lineComment != null && code.startsWith(lineComment, i)) {
                    val end = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                    withStyle(SpanStyle(color = palette.comment)) { append(code.substring(i, end)) }
                    i = end
                    continue
                }
                if (code.startsWith("/*", i)) {
                    val end = code.indexOf("*/", i + 2).let { if (it < 0) code.length else it + 2 }
                    withStyle(SpanStyle(color = palette.comment)) { append(code.substring(i, end)) }
                    i = end
                    continue
                }
                if (language == "html" && code.startsWith("<!--", i)) {
                    val end = code.indexOf("-->", i + 4).let { if (it < 0) code.length else it + 3 }
                    withStyle(SpanStyle(color = palette.comment)) { append(code.substring(i, end)) }
                    i = end
                    continue
                }
                if (code.startsWith("\"\"\"", i) || code.startsWith("'''", i)) {
                    val token = code.substring(i, i + 3)
                    val end = code.indexOf(token, i + 3).let { if (it < 0) code.length else it + 3 }
                    withStyle(SpanStyle(color = palette.string)) { append(code.substring(i, end)) }
                    i = end
                    continue
                }
                if (c == '"' || c == '\'' || c == '`') {
                    var end = i + 1
                    while (end < code.length) {
                        if (code[end] == '\\') { end += 2; continue }
                        if (code[end] == c) { end++; break }
                        if (code[end] == '\n' && c != '`') break
                        end++
                    }
                    withStyle(SpanStyle(color = palette.string)) { append(code.substring(i, minOf(end, code.length))) }
                    i = minOf(end, code.length)
                    continue
                }
                if (c.isDigit() && (i == 0 || !code[i - 1].isLetterOrDigit())) {
                    var end = i
                    while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '.' || code[end] == '_')) end++
                    withStyle(SpanStyle(color = palette.number)) { append(code.substring(i, end)) }
                    i = end
                    continue
                }
                if (c.isLetter() || c == '_' || c == '$') {
                    var end = i
                    while (end < code.length && (code[end].isLetterOrDigit() || code[end] == '_' || code[end] == '$')) end++
                    val word = code.substring(i, end)
                    val isCall = code.getOrNull(end) == '('
                    val color = when {
                        word in keywords -> palette.keyword
                        word in typeKeywords -> palette.type
                        isCall -> palette.function
                        word.firstOrNull()?.isUpperCase() == true -> palette.type
                        else -> palette.plain
                    }
                    withStyle(SpanStyle(color = color)) { append(word) }
                    i = end
                    continue
                }
                withStyle(SpanStyle(color = palette.plain)) { append(c) }
                i++
            }
        }
    }

    private fun normalize(lang: String): String = when (lang.lowercase().trim()) {
        "kt", "kts" -> "kotlin"
        "py" -> "python"
        "js", "jsx", "mjs" -> "javascript"
        "ts", "tsx" -> "typescript"
        "sh", "bash", "zsh", "console" -> "shell"
        "yml" -> "yaml"
        "c++", "cc", "cxx", "hpp" -> "cpp"
        "c#" -> "csharp"
        "rs" -> "rust"
        "rb" -> "ruby"
        "md" -> "markdown"
        "" -> "text"
        else -> lang.lowercase().trim()
    }

    private fun keywordsFor(language: String): Set<String> = commonKeywords
}
