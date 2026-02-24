package com.mocharealm.compound.domain.model

//Old Code
//data class Message(
//    val id: Long,
//    val chatId: Long,
//    val senderId: Long,
//    val senderName: String,
//    val content: String,
//    val timestamp: Long,
//    val isOutgoing: Boolean = false,
//    val messageType: MessageType = MessageType.TEXT,
//    val fileUrl: String? = null,
//    val fileId: Int? = null,
//    val avatarUrl: String? = null,
//    val stickerFormat: StickerFormat? = null,
//    val entities: List<TextEntity> = emptyList(),
//    val replyTo: ReplyInfo? = null,
//    val mediaAlbumId: Long = 0L,
//    val hasSpoiler: Boolean = false,
//    val thumbnailFileId: Int? = null,
//    val thumbnailUrl: String? = null,
//    val mediaWidth: Int = 0,
//    val mediaHeight: Int = 0,
//    val shareInfo: ShareInfo? = null
//)
//
//enum class MessageType {
//    TEXT,
//    PHOTO,
//    VIDEO,
//    DOCUMENT,
//    AUDIO,
//    VOICE,
//    STICKER,
//    SYSTEM,
//    VENUE
//}
//
//// TODO: Support all types
//enum class SystemActionType {
//    MEMBER_JOINED,         // un1 (who added), un2 (who was added)
//    MEMBER_JOINED_BY_LINK, // un1 (who joined)
//    MEMBER_LEFT,           // un1 (who left/was removed)
//    CHAT_CHANGED_TITLE,    // un1 (who changed), title (new title)
//    CHAT_CHANGED_PHOTO,    // un1 (who changed)
//    CHAT_UPGRADED_TO,      // supergroupId
//    PIN_MESSAGE
//}
//
//enum class StickerFormat {
//    WEBP,
//    TGS,
//    WEBM,
//    MP4,
//    GIF
//}
//
//data class ReplyInfo(
//    val messageId: Long,
//    val senderName: String,
//    val text: String
//)
//
//data class TextEntity(
//    val offset: Int,
//    val length: Int,
//    val type: TextEntityType
//)
//
//sealed class TextEntityType {
//    data object Bold : TextEntityType()
//    data object Italic : TextEntityType()
//    data object Underline : TextEntityType()
//    data object Strikethrough : TextEntityType()
//    data object Code : TextEntityType()
//    data object Pre : TextEntityType()
//    data class PreCode(val language: String) : TextEntityType()
//    data class TextUrl(val url: String) : TextEntityType()
//    data object Url : TextEntityType()
//    data object Mention : TextEntityType()
//    data object Spoiler : TextEntityType()
//    data object EmailAddress : TextEntityType()
//    data object PhoneNumber : TextEntityType()
//}


data class Message(
    val senderId: Long,
    val chatId: Long,
    val blocks: List<MessageBlock>,
    val replyTo: Message? = null,
    val shareInfo: ShareInfo? = null
)

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
        val mediaWidth: Int = 0,
        val mediaHeight: Int = 0,
        val file: File,
        val thumbnail: File? = null,
        val caption: Text? = null,
    ) : MessageBlock {
        enum class MediaType {
            PHOTO, VIDEO
        }
    }

    data class StickerBlock(
        override val id: Long,
        override val timestamp: Long,
        val stickerFormat: StickerFormat? = null,
        val file: File,
        val caption: Text
    ) : MessageBlock {
        enum class StickerFormat {
            WEBP, TGS, WEBM, MP4, GIF
        }
    }

    data class SystemActionBlock(
        override val id: Long,
        override val timestamp: Long,
        val type: SystemActionType
    ): MessageBlock {
        // TODO: Support all types
        sealed interface SystemActionType {
            data class MemberJoined(val actorId: Long, val actorName: String, val targetId: Long, val targetName: String) : SystemActionType
            data class MemberJoinedByLink(val userId: Long, val userName: String) : SystemActionType
            data class ChatChangedTitle(val actorName: String, val newTitle: String) : SystemActionType
            data class PinMessage(val actorName: String, val messagePreview: String?) : SystemActionType
        }
    }

    data class VenueBlock(
        override val id: Long,
        override val timestamp: Long,
        val venue: Venue
    ): MessageBlock
}