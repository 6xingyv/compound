package com.mocharealm.compound.ui.util

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock

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
