package com.mocharealm.compound.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.mocharealm.compound.domain.model.Text

object RichTextTags {
    const val URL = "URL"
    const val MENTION = "MENTION"
    const val SPOILER = "SPOILER"
    const val CUSTOM_EMOJI = "CUSTOM_EMOJI"
    const val INLINE_CONTENT = "androidx.compose.foundation.text.inlineContent"
}

/**
 * Converts a plain text string with Telegram-style [Text.TextEntity] spans into a Compose
 * [AnnotatedString] with appropriate [SpanStyle] and annotations.
 */
fun Text.toAnnotatedString(
    linkColor: Color = Color(0xFF5B9BD5),
): AnnotatedString {
    if (entities.isEmpty()) return AnnotatedString(content)

    return buildAnnotatedString {
        append(content)

        entities.forEachIndexed { index, entity ->
            // Skip Compound share protocol entities (invisible metadata)
            if (entity.type is Text.TextEntityType.TextUrl &&
                entity.type.url.startsWith("https://compound.mocharealm.com/share")
            )
                return@forEachIndexed

            val start = entity.offset
            val end = (entity.offset + entity.length).coerceAtMost(content.length)
            if (start >= content.length || start >= end) return@forEachIndexed

            // Apply the visual style
            addStyle(
                TextEntityStyle.getStyle(type = entity.type, linkColor = linkColor),
                start,
                end
            )

            // Apply annotations for clickable types
            when (val type = entity.type) {
                is Text.TextEntityType.TextUrl -> {
                    addStringAnnotation(RichTextTags.URL, type.url, start, end)
                }
                is Text.TextEntityType.Url -> {
                    addStringAnnotation(RichTextTags.URL, content.substring(start, end), start, end)
                }
                is Text.TextEntityType.EmailAddress -> {
                    addStringAnnotation(
                        RichTextTags.URL,
                        "mailto:${content.substring(start, end)}",
                        start,
                        end
                    )
                }
                is Text.TextEntityType.Mention -> {
                    addStringAnnotation(RichTextTags.MENTION, content.substring(start, end), start, end)
                }
                is Text.TextEntityType.Spoiler -> {
                    addStringAnnotation(RichTextTags.SPOILER, index.toString(), start, end)
                }
                is Text.TextEntityType.CustomEmoji -> {
                    addStringAnnotation(RichTextTags.CUSTOM_EMOJI, type.customEmojiId.toString(), start, end)
                    addStringAnnotation(RichTextTags.INLINE_CONTENT, type.customEmojiId.toString(), start, end)
                }
                else -> {}
            }
        }
    }
}
