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

            when (entity.type) {
                is TextEntityType.Bold -> addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold), start, end
                )
                is TextEntityType.Italic -> addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic), start, end
                )
                is TextEntityType.Underline -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline), start, end
                )
                is TextEntityType.Strikethrough -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough), start, end
                )
                is TextEntityType.Code, is TextEntityType.Pre -> addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace), start, end
                )
                is TextEntityType.PreCode -> addStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace), start, end
                )
                is TextEntityType.TextUrl -> {
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        start, end
                    )
                    addStringAnnotation(URL_ANNOTATION_TAG, entity.type.url, start, end)
                }
                is TextEntityType.Url -> {
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        start, end
                    )
                    addStringAnnotation(URL_ANNOTATION_TAG, text.substring(start, end), start, end)
                }
                is TextEntityType.EmailAddress -> {
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        start, end
                    )
                    addStringAnnotation(URL_ANNOTATION_TAG, "mailto:${text.substring(start, end)}", start, end)
                }
                is TextEntityType.Mention -> addStyle(
                    SpanStyle(color = linkColor), start, end
                )
                is TextEntityType.PhoneNumber -> addStyle(
                    SpanStyle(color = linkColor), start, end
                )
                is TextEntityType.Spoiler -> addStyle(
                    SpanStyle(
                        color = Color.Transparent,
                        background = Color.Gray,
                    ), start, end
                )
            }
        }
    }
}
