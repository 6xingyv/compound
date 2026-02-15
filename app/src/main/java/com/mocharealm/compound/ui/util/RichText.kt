package com.mocharealm.compound.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.TextEntityType

/**
 * Annotation tag used for clickable URLs.
 */
const val URL_ANNOTATION_TAG = "URL"

/**
 * Converts a plain text string with Telegram-style [TextEntity] spans into a Compose
 * [AnnotatedString] with appropriate [SpanStyle] and URL annotations.
 *
 * TDLib provides offsets/lengths in UTF-16 code units, which matches Kotlin's
 * [String.length] (char count), so we can use them directly as indices.
 */
fun buildAnnotatedString(
    text: String,
    entities: List<TextEntity>,
    linkColor: Color = Color(0xFF5B9BD5),
): AnnotatedString {
    if (entities.isEmpty()) return AnnotatedString(text)

    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)

        for (entity in entities) {
            val start = entity.offset
            val end = (entity.offset + entity.length).coerceAtMost(text.length)
            if (start >= text.length || start >= end) continue

            // Apply the visual style
            addStyle(TextEntityStyle.getStyle(entity.type, linkColor), start, end)

            // Apply annotations for clickable types
            when (entity.type) {
                is TextEntityType.TextUrl -> {
                    addStringAnnotation(URL_ANNOTATION_TAG, entity.type.url, start, end)
                }
                is TextEntityType.Url -> {
                    addStringAnnotation(URL_ANNOTATION_TAG, text.substring(start, end), start, end)
                }
                is TextEntityType.EmailAddress -> {
                    addStringAnnotation(URL_ANNOTATION_TAG, "mailto:${text.substring(start, end)}", start, end)
                }
                else -> {}
            }
        }
    }
}
