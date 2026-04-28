package com.onmi.qing.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    markdown: String,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val style = MaterialTheme.typography.bodyMedium.copy(color = color)

    Text(
        text = parseMarkdown(markdown),
        style = style,
        modifier = modifier
    )
}

private fun parseMarkdown(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            // Check for code block start/end
            if (line.trimStart().startsWith("```")) {
                val codeLines = mutableListOf<String>()
                val codeStart = if (line.trimStart().startsWith("```")) line else ""
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                // Render code block
                if (codeLines.isNotEmpty() || codeStart.isNotEmpty()) {
                    append("\n")
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
                    codeLines.forEachIndexed { index, codeLine ->
                        append(codeLine)
                        if (index < codeLines.size - 1) append("\n")
                    }
                    pop()
                    append("\n")
                }
                i++ // Skip closing ```
                continue
            }

            // Check for headers
            val headerMatch = Regex("^(#{1,6})\\s+(.+)$").find(line)
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val content = headerMatch.groupValues[2]
                val fontSize = when (level) {
                    1 -> 24.sp
                    2 -> 20.sp
                    3 -> 18.sp
                    else -> 16.sp
                }
                append("\n")
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = fontSize))
                appendInlineStyles(content)
                pop()
                append("\n")
                i++
                continue
            }

            // Check for bullet list
            val bulletMatch = Regex("^[-*]\\s+(.+)$").find(line)
            if (bulletMatch != null) {
                append("• ")
                appendInlineStyles(bulletMatch.groupValues[1])
                append("\n")
                i++
                continue
            }

            // Check for numbered list
            val numberedMatch = Regex("^\\d+\\.\\s+(.+)$").find(line)
            if (numberedMatch != null) {
                appendInlineStyles(numberedMatch.groupValues[1])
                append("\n")
                i++
                continue
            }

            // Check for blockquote
            if (line.trimStart().startsWith(">")) {
                append("│ ")
                appendInlineStyles(line.removePrefix(">").removePrefix(" "))
                append("\n")
                i++
                continue
            }

            // Check for horizontal rule
            if (Regex("^[-*_]{3,}$").matches(line.trim())) {
                append("────────────────────\n")
                i++
                continue
            }

            // Empty line
            if (line.isBlank()) {
                append("\n")
                i++
                continue
            }

            // Regular paragraph line
            appendInlineStyles(line)
            append("\n")
            i++
        }
    }
}

private fun AnnotatedString.Builder.appendInlineStyles(text: String) {
    var i = 0
    val length = text.length

    while (i < length) {
        // Check for bold **text** (highest priority, check first)
        val boldMatch = Regex("\\*\\*(.+?)\\*\\*").find(text, i)
        if (boldMatch != null && boldMatch.range.first == i) {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(boldMatch.groupValues[1])
            pop()
            i += boldMatch.value.length
            continue
        }

        // Check for inline code `code`
        val codeMatch = Regex("`([^`]+)`").find(text, i)
        if (codeMatch != null && codeMatch.range.first == i) {
            pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            append(codeMatch.groupValues[1])
            pop()
            i += codeMatch.value.length
            continue
        }

        // Check for italic *text* (single * only, not **)
        val italicMatch = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)").find(text, i)
        if (italicMatch != null && italicMatch.range.first == i) {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append(italicMatch.groupValues[1])
            pop()
            i += italicMatch.value.length
            continue
        }

        // Check for strikethrough ~~text~~
        val strikeMatch = Regex("~~(.+?)~~").find(text, i)
        if (strikeMatch != null && strikeMatch.range.first == i) {
            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            append(strikeMatch.groupValues[1])
            pop()
            i += strikeMatch.value.length
            continue
        }

        // No marker found at current position, append single char
        append(text[i])
        i++
    }
}
