package com.mocharealm.compound.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.util.formatName
import com.mocharealm.tci18n.core.tdString

/** Extracts a human-readable preview with styles from a [Message] for display in the chat list. */
@Composable
fun Message.toPreviewAnnotatedString(chat: Chat): AnnotatedString {
    val senderPrefix = when {
        isOutgoing -> {
            val fromYou = tdString("FromYou")
            "$fromYou: "
        }
        chat.type == ChatType.GROUP -> "${sender.formatName()}: "
        else -> ""
    }

    val first = blocks.firstOrNull() ?: return AnnotatedString("")

    // If there's a caption TextBlock after a media/doc block, prefer it as preview
    val captionBlock =
        if (first !is MessageBlock.TextBlock) {
            blocks.filterIsInstance<MessageBlock.TextBlock>()
                .firstOrNull()
                ?.takeIf { it.content.content.isNotEmpty() }
        } else null

    return buildAnnotatedString {
        if (senderPrefix.isNotEmpty()) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(senderPrefix)
            }
        }

        when {
            first is MessageBlock.TextBlock -> {
                append(first.content.toAnnotatedString())
            }
            captionBlock != null -> {
                append(captionBlock.content.toAnnotatedString())
            }
            first is MessageBlock.MediaBlock -> {
                append(
                    when (first.mediaType) {
                        MessageBlock.MediaBlock.MediaType.PHOTO -> "Photo"
                        MessageBlock.MediaBlock.MediaType.VIDEO -> "Video"
                    }
                )
            }
            first is MessageBlock.DocumentBlock -> {
                append(first.document.fileName)
            }
            first is MessageBlock.StickerBlock -> {
                append("Sticker")
            }
            first is MessageBlock.SystemActionBlock -> {
                append(
                    when (val type = first.type) {
                        is MessageBlock.SystemActionBlock.SystemActionType.MemberJoined -> {
                            "${type.actorName} added ${type.targetName}"
                        }
                        is MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink -> {
                            "${type.userName} joined via link"
                        }
                        is MessageBlock.SystemActionBlock.SystemActionType.ChatChangedTitle -> {
                            "${type.actorName} changed title to ${type.newTitle}"
                        }
                        is MessageBlock.SystemActionBlock.SystemActionType.PinMessage -> {
                            "${type.actorName} pinned a message"
                        }
                    }
                )
            }
            first is MessageBlock.VenueBlock -> {
                append(first.venue.name)
            }
        }
    }
}

/** Extracts a human-readable preview text from a [Message] for display in the chat list. */
fun Message.toPreviewText(): String {
    val first = blocks.firstOrNull() ?: return ""
    // If there's a caption TextBlock after a media/doc block, prefer it as preview
    val captionText =
        if (first !is MessageBlock.TextBlock) {
            blocks.filterIsInstance<MessageBlock.TextBlock>()
                .firstOrNull()
                ?.content
                ?.content
                ?.ifEmpty { null }
        } else null

    return when (first) {
        is MessageBlock.TextBlock -> first.content.content
        is MessageBlock.MediaBlock ->
            when (first.mediaType) {
                MessageBlock.MediaBlock.MediaType.PHOTO -> captionText ?: "Photo"
                MessageBlock.MediaBlock.MediaType.VIDEO -> captionText ?: "Video"
            }
        is MessageBlock.DocumentBlock -> captionText ?: first.document.fileName
        is MessageBlock.StickerBlock -> "Sticker"
        is MessageBlock.SystemActionBlock ->
            when (first.type) {
                is MessageBlock.SystemActionBlock.SystemActionType.MemberJoined -> {
                    val t = first.type
                    "${t.actorName} added ${t.targetName}"
                }
                is MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink -> {
                    val t = first.type
                    "${t.userName} joined via link"
                }
                is MessageBlock.SystemActionBlock.SystemActionType.ChatChangedTitle -> {
                    val t = first.type
                    "${t.actorName} changed title to ${t.newTitle}"
                }
                is MessageBlock.SystemActionBlock.SystemActionType.PinMessage -> {
                    val t = first.type
                    "${t.actorName} pinned a message"
                }
            }
        is MessageBlock.VenueBlock -> first.venue.name
    }
}
