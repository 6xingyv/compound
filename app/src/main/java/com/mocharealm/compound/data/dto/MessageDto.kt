package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.StickerFormat

data class MessageDto(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,
    val fileId: Int? = null,
    val avatarUrl: String? = null,
    val stickerFormat: StickerFormat? = null
) {
    fun toDomain(): Message = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        timestamp = timestamp,
        isOutgoing = isOutgoing,
        messageType = messageType,
        fileUrl = null,
        fileId = fileId,
        avatarUrl = avatarUrl,
        stickerFormat = stickerFormat
    )
}