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
    val stickerFormat: StickerFormat? = null
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