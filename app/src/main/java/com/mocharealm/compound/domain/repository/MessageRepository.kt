package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.Text
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getChatMessages(
        chatId: Long,
        limit: Int = 20,
        fromMessageId: Long = 0,
        onlyLocal: Boolean = false,
        offset: Int = 0
    ): Result<List<Message>>

    suspend fun sendMessage(
        chatId: Long,
        text: String,
        entities: List<Text.TextEntity> = emptyList(),
        replyToMessageId: Long = 0
    ): Result<Message>

    suspend fun sendFiles(
        chatId: Long,
        files: List<ShareFileInfo>,
        caption: String = "",
        captionEntities: List<Text.TextEntity> = emptyList(),
        replyToMessageId: Long = 0
    ): Result<List<Message>>

    val messageUpdates: Flow<MessageUpdateEvent>

    suspend fun sendSticker(chatId: Long, sticker: MessageBlock.StickerBlock): Result<Message>
    suspend fun sendLocation(chatId: Long, latitude: Double, longitude: Double): Result<Message>
    suspend fun setChatDraftMessage(
        chatId: Long,
        replyToMessageId: Long,
        draftText: String
    ): Result<Unit>
}
