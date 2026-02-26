package com.mocharealm.compound.ui.util

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock

/** Extracts a human-readable preview text from a [Message] for display in the chat list. */
fun Message.toPreviewText(): String {
    val block = blocks.firstOrNull() ?: return ""
    return block.previewText()
}

private fun MessageBlock.previewText(): String =
        when (this) {
            is MessageBlock.TextBlock -> content.content
            is MessageBlock.MediaBlock ->
                    when (mediaType) {
                        MessageBlock.MediaBlock.MediaType.PHOTO ->
                                caption?.content?.ifEmpty { null } ?: "Photo"
                        MessageBlock.MediaBlock.MediaType.VIDEO ->
                                caption?.content?.ifEmpty { null } ?: "Video"
                    }
            is MessageBlock.DocumentBlock -> caption?.content?.ifEmpty { null } ?: document.fileName
            is MessageBlock.StickerBlock -> "Sticker"
            is MessageBlock.SystemActionBlock ->
                    when (type) {
                        is MessageBlock.SystemActionBlock.SystemActionType.MemberJoined -> {
                            val t =
                                type
                            "${t.actorName} added ${t.targetName}"
                        }
                        is MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink -> {
                            val t =
                                type
                            "${t.userName} joined via link"
                        }
                        is MessageBlock.SystemActionBlock.SystemActionType.ChatChangedTitle -> {
                            val t =
                                type
                            "${t.actorName} changed title to ${t.newTitle}"
                        }
                        is MessageBlock.SystemActionBlock.SystemActionType.PinMessage -> {
                            val t =
                                type
                            "${t.actorName} pinned a message"
                        }
                    }
            is MessageBlock.VenueBlock -> venue.name
        }
