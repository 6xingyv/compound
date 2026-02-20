package com.mocharealm.compound.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
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
    revealedEntityIndices: Set<Int> = emptySet(),
): AnnotatedString {
    if (entities.isEmpty()) return AnnotatedString(text)

    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)

        entities.forEachIndexed { index, entity ->
            // Skip Compound share protocol entities (invisible metadata)
            if (entity.type is TextEntityType.TextUrl &&
                entity.type.url.startsWith("https://compound.mocharealm.com/share")
            ) return@forEachIndexed

            val start = entity.offset
            val end = (entity.offset + entity.length).coerceAtMost(text.length)
            if (start >= text.length || start >= end) return@forEachIndexed

            // Apply the visual style
            addStyle(
                TextEntityStyle.getStyle(
                    type = entity.type, 
                    linkColor = linkColor
                ),
                start, 
                end
            )

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
                is TextEntityType.Mention -> {
                    addStringAnnotation("MENTION", text.substring(start, end), start, end)
                }
                is TextEntityType.Spoiler -> {
                    addStringAnnotation("SPOILER", index.toString(), start, end)
                }
                else -> {}
            }
        }
    }
}
