package com.mocharealm.compound.domain.model

data class Message(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,
    val fileUrl: String? = null,
    val fileId: Int? = null,
    val avatarUrl: String? = null,
    val stickerFormat: StickerFormat? = null,
    val entities: List<TextEntity> = emptyList()
)

enum class MessageType {
    TEXT,
    PHOTO,
    VIDEO,
    DOCUMENT,
    AUDIO,
    VOICE,
    STICKER
}

enum class StickerFormat {
    WEBP,
    TGS,
    WEBM
}

data class TextEntity(
    val offset: Int,
    val length: Int,
    val type: TextEntityType
)

sealed class TextEntityType {
    data object Bold : TextEntityType()
    data object Italic : TextEntityType()
    data object Underline : TextEntityType()
    data object Strikethrough : TextEntityType()
    data object Code : TextEntityType()
    data object Pre : TextEntityType()
    data class PreCode(val language: String) : TextEntityType()
    data class TextUrl(val url: String) : TextEntityType()
    data object Url : TextEntityType()
    data object Mention : TextEntityType()
    data object Spoiler : TextEntityType()
    data object EmailAddress : TextEntityType()
    data object PhoneNumber : TextEntityType()
}