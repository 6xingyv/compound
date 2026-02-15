package com.mocharealm.compound.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mocharealm.compound.domain.model.TextEntityType

object TextEntityStyle {
    
    fun getStyle(
        type: TextEntityType, 
        linkColor: Color = Color(0xFF5B9BD5),
        codeBackgroundColor: Color = Color.Gray.copy(alpha = 0.15f)
    ): SpanStyle {
        return when (type) {
            is TextEntityType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
            is TextEntityType.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
            is TextEntityType.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
            is TextEntityType.Strikethrough -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            is TextEntityType.Code, is TextEntityType.Pre, is TextEntityType.PreCode -> 
                SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackgroundColor)
            is TextEntityType.TextUrl, is TextEntityType.Url, is TextEntityType.EmailAddress -> 
                SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
            is TextEntityType.Mention, is TextEntityType.PhoneNumber -> 
                SpanStyle(color = linkColor)
            is TextEntityType.Spoiler -> SpanStyle(color = Color.Transparent, background = Color.Gray)
        }
    }
}
