package com.mocharealm.compound.ui.util

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import com.mocharealm.compound.domain.util.MarkdownParser

fun parseMarkdown(text: CharSequence): AnnotatedString {
    val builder = AnnotatedString.Builder(text.toString())
    val matches = MarkdownParser.findMatches(text)
    
    for (match in matches) {
        // Apply main style
        builder.addStyle(TextEntityStyle.getStyle(match.type), match.fullRange.first, match.fullRange.last + 1)
        
        // De-emphasize symbols
        val symbolStyle = SpanStyle(color = Color.Gray.copy(alpha = 0.5f))
        
        // Prefix symbols (start of full range to start of content)
        if (match.contentRange.first > match.fullRange.first) {
            builder.addStyle(symbolStyle, match.fullRange.first, match.contentRange.first)
        }
        
        // Suffix symbols (end of content to end of full range)
        if (match.contentRange.last < match.fullRange.last) {
            builder.addStyle(symbolStyle, match.contentRange.last + 1, match.fullRange.last + 1)
        }
    }
    return builder.toAnnotatedString()
}

val MarkdownTransformation = OutputTransformation {
    val annotated = parseMarkdown(asCharSequence())
    
    annotated.spanStyles.forEach { range ->
        addStyle(range.item, range.start, range.end)
    }
}
