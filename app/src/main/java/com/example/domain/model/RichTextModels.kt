package com.example.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class BlockType {
    NORMAL,
    HEADING_1,
    HEADING_2,
    QUOTE,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKLIST
}

@Serializable
enum class InlineMarkType {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    LINK
}

@Serializable
data class InlineSpan(
    val start: Int,
    val end: Int,
    val type: InlineMarkType,
    val url: String? = null
)

@Serializable
data class RichBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType = BlockType.NORMAL,
    val text: String = "",
    val checked: Boolean = false,
    val spans: List<InlineSpan> = emptyList()
)

@Serializable
data class RichDocument(
    val blocks: List<RichBlock> = listOf(RichBlock())
) {
    fun toPlainText(): String {
        return blocks.joinToString("\n") { block ->
            when (block.type) {
                BlockType.BULLET_LIST -> "• ${block.text}"
                BlockType.CHECKLIST -> if (block.checked) "[x] ${block.text}" else "[ ] ${block.text}"
                BlockType.QUOTE -> "> ${block.text}"
                else -> block.text
            }
        }.trim()
    }

    fun toMarkdown(): String {
        var numberIndex = 1
        return blocks.joinToString("\n\n") { block ->
            if (block.type != BlockType.NUMBERED_LIST) {
                numberIndex = 1
            }
            val styledText = applySpansToMarkdown(block.text, block.spans)
            when (block.type) {
                BlockType.HEADING_1 -> "# $styledText"
                BlockType.HEADING_2 -> "## $styledText"
                BlockType.QUOTE -> "> $styledText"
                BlockType.BULLET_LIST -> "- $styledText"
                BlockType.NUMBERED_LIST -> "${numberIndex++}. $styledText"
                BlockType.CHECKLIST -> if (block.checked) "- [x] $styledText" else "- [ ] $styledText"
                BlockType.NORMAL -> styledText
            }
        }.trim()
    }

    fun wordCount(): Int {
        val raw = blocks.joinToString(" ") { it.text }.trim()
        if (raw.isEmpty()) return 0
        return raw.split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun charCount(): Int = blocks.sumOf { it.text.length }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun encode(doc: RichDocument): String = json.encodeToString(doc)

        fun decode(raw: String): RichDocument {
            if (raw.isBlank()) return RichDocument()
            return try {
                val parsed = json.decodeFromString<RichDocument>(raw)
                if (parsed.blocks.isEmpty()) RichDocument() else parsed
            } catch (_: Exception) {
                RichDocument(
                    blocks = raw.split("\n").map { line ->
                        RichBlock(text = line)
                    }.ifEmpty { listOf(RichBlock()) }
                )
            }
        }

        fun fromPlainText(text: String): RichDocument {
            val lines = text.split("\n")
            return RichDocument(
                blocks = lines.map { RichBlock(text = it) }.ifEmpty { listOf(RichBlock()) }
            )
        }
    }
}

private fun applySpansToMarkdown(text: String, spans: List<InlineSpan>): String {
    if (spans.isEmpty() || text.isEmpty()) return text
    var result = text
    // Basic readable markdown rendering when exporting
    val sorted = spans.sortedByDescending { it.start }
    for (span in sorted) {
        val s = span.start.coerceIn(0, result.length)
        val e = span.end.coerceIn(s, result.length)
        if (s >= e) continue
        val slice = result.substring(s, e)
        val wrapped = when (span.type) {
            InlineMarkType.BOLD -> "**$slice**"
            InlineMarkType.ITALIC -> "*$slice*"
            InlineMarkType.UNDERLINE -> "<u>$slice</u>"
            InlineMarkType.STRIKETHROUGH -> "~~$slice~~"
            InlineMarkType.LINK -> "[$slice](${span.url ?: slice})"
        }
        result = result.replaceRange(s, e, wrapped)
    }
    return result
}

fun RichBlock.toAnnotatedString(
    linkColor: Color,
    searchQuery: String = "",
    searchHighlightBg: Color = Color(0x66F5B700)
): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        spans.forEach { span ->
            val safeStart = span.start.coerceIn(0, text.length)
            val safeEnd = span.end.coerceIn(safeStart, text.length)
            if (safeStart < safeEnd) {
                when (span.type) {
                    InlineMarkType.BOLD -> addStyle(
                        SpanStyle(fontWeight = FontWeight.Bold),
                        safeStart,
                        safeEnd
                    )
                    InlineMarkType.ITALIC -> addStyle(
                        SpanStyle(fontStyle = FontStyle.Italic),
                        safeStart,
                        safeEnd
                    )
                    InlineMarkType.UNDERLINE -> addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        safeStart,
                        safeEnd
                    )
                    InlineMarkType.STRIKETHROUGH -> addStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                        safeStart,
                        safeEnd
                    )
                    InlineMarkType.LINK -> {
                        addStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline
                            ),
                            safeStart,
                            safeEnd
                        )
                        span.url?.let { url ->
                            addStringAnnotation(
                                tag = "URL",
                                annotation = url,
                                start = safeStart,
                                end = safeEnd
                            )
                        }
                    }
                }
            }
        }
        if (searchQuery.isNotBlank()) {
            var idx = text.indexOf(searchQuery, ignoreCase = true)
            while (idx >= 0) {
                addStyle(
                    SpanStyle(background = searchHighlightBg),
                    idx,
                    (idx + searchQuery.length).coerceAtMost(text.length)
                )
                idx = text.indexOf(searchQuery, startIndex = idx + searchQuery.length, ignoreCase = true)
            }
        }
    }
}
