package com.mocharealm.compound.domain.model

data class Message(
    val sender: User,
    val chatId: Long,
    val isOutgoing: Boolean = false,
    val blocks: List<MessageBlock>,
    val replyTo: Message? = null,
    val shareInfo: ShareInfo? = null
) {
    val id: Long
        get() = blocks.firstOrNull()?.id ?: 0L
    val timestamp: Long
        get() = blocks.firstOrNull()?.timestamp ?: 0L
}

sealed interface MessageBlock {
    val id: Long
    val timestamp: Long

    data class TextBlock(
        override val id: Long,
        override val timestamp: Long,
        val content: Text,
    ) : MessageBlock

    data class MediaBlock(
        override val id: Long,
        override val timestamp: Long,
        val mediaType: MediaType,
        val width: Int = 0,
        val height: Int = 0,
        val file: File,
        val thumbnail: File? = null,
        val hasSpoiler: Boolean = false,
        val mediaAlbumId: Long = 0L,
    ) : MessageBlock {
        enum class MediaType {
            PHOTO,
            VIDEO
        }
    }

    data class DocumentBlock(
        override val id: Long,
        override val timestamp: Long,
        val document: Document,
        val mediaAlbumId: Long = 0L,
    ) : MessageBlock

    data class StickerBlock(
        override val id: Long,
        override val timestamp: Long,
        val stickerFormat: StickerFormat? = null,
        val file: File,
        val thumbnail: File? = null,
        val caption: Text
    ) : MessageBlock {
        enum class StickerFormat {
            WEBP,
            TGS,
            WEBM,
            MP4,
            GIF
        }
    }

    data class SystemActionBlock(
        override val id: Long,
        override val timestamp: Long,
        val type: SystemActionType
    ) : MessageBlock {
        sealed interface SystemActionType {
            data class MemberJoined(
                val actorId: Long,
                val actorName: String,
                val targetId: Long,
                val targetName: String
            ) : SystemActionType

            data class MemberJoinedByLink(val userId: Long, val userName: String) :
                SystemActionType

            data class ChatChangedTitle(val actorName: String, val newTitle: String) :
                SystemActionType

            data class PinMessage(val actorName: String, val messagePreview: String?) :
                SystemActionType
        }
    }

    data class VenueBlock(
        override val id: Long,
        override val timestamp: Long,
        val venue: Venue
    ) : MessageBlock
}
