package com.mocharealm.compound.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mocharealm.compound.domain.model.Text

object TextEntityStyle {
    fun getStyle(
            type: Text.TextEntityType,
            linkColor: Color = Color(0xFF5B9BD5),
            codeBackgroundColor: Color = Color.Gray.copy(alpha = 0.15f)
    ): SpanStyle {
        return when (type) {
            is Text.TextEntityType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
            is Text.TextEntityType.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
            is Text.TextEntityType.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
            is Text.TextEntityType.Strikethrough ->
                    SpanStyle(textDecoration = TextDecoration.LineThrough)
            is Text.TextEntityType.Code,
            is Text.TextEntityType.Pre,
            is Text.TextEntityType.PreCode ->
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackgroundColor)
            is Text.TextEntityType.TextUrl,
            is Text.TextEntityType.Url,
            is Text.TextEntityType.EmailAddress ->
                    SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            is Text.TextEntityType.Mention, is Text.TextEntityType.PhoneNumber ->
                    SpanStyle(color = linkColor)
            is Text.TextEntityType.Spoiler, is Text.TextEntityType.CustomEmoji -> SpanStyle()
        }
    }
}
