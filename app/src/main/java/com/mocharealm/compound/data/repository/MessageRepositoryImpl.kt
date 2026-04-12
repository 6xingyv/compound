package com.mocharealm.compound.data.repository

import com.mocharealm.compound.data.dto.MessageDto
import com.mocharealm.compound.data.mapper.MessageMapper
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.repository.MessageRepository
import com.mocharealm.compound.domain.util.MarkdownParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transform
import org.drinkless.tdlib.TdApi

class MessageRepositoryImpl(
    private val tdLibDataSource: TdLibDataSource,
    private val messageMapper: MessageMapper
) : MessageRepository {

    override suspend fun getChatMessages(
        chatId: Long,
        limit: Int,
        fromMessageId: Long,
        onlyLocal: Boolean,
        offset: Int
    ): Result<List<Message>> = runCatching {
        val result = tdLibDataSource.send(TdApi.GetChatHistory(chatId, fromMessageId, offset, limit, onlyLocal))
        if (result !is TdApi.Messages) error("Invalid messages response type")

        val rawMessages = result.messages.map { messageMapper.mapSingleTdMessage(it) }
        messageMapper.aggregateAlbums(rawMessages)
    }

    override suspend fun sendMessage(
        chatId: Long,
        text: String,
        entities: List<com.mocharealm.compound.domain.model.Text.TextEntity>,
        replyToMessageId: Long
    ): Result<Message> {
        return try {
            val (finalText, finalEntities) = if (entities.isEmpty()) {
                MarkdownParser.parseForSending(text)
            } else {
                text to entities
            }

            val content = TdApi.InputMessageText(
                TdApi.FormattedText(finalText, MessageDto.mapToTdApiEntities(finalEntities)),
                null,
                true
            )

            val replyTo = if (replyToMessageId != 0L) {
                TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0)
            } else null

            val sentMessage = tdLibDataSource.send(TdApi.SendMessage(chatId, null, replyTo, null, null, content))
            Result.success(messageMapper.mapSingleTdMessage(sentMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendFiles(
        chatId: Long,
        files: List<ShareFileInfo>,
        caption: String,
        captionEntities: List<com.mocharealm.compound.domain.model.Text.TextEntity>,
        replyToMessageId: Long
    ): Result<List<Message>> = runCatching {
        val formattedCaption = TdApi.FormattedText(caption, MessageDto.mapToTdApiEntities(captionEntities))

        val contents = files.mapIndexed { index, file ->
            val isLast = index == files.lastIndex
            val cap = if (isLast) formattedCaption else TdApi.FormattedText("", emptyArray())
            val thumbnail = file.thumbnailPath?.let { TdApi.InputThumbnail(TdApi.InputFileLocal(it), 320, 320) }

            TdApi.InputMessageDocument(TdApi.InputFileLocal(file.filePath), thumbnail, false, cap)
        }

        val replyTo = if (replyToMessageId != 0L) {
            TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0)
        } else null

        if (contents.size == 1) {
            val msg = tdLibDataSource.send(TdApi.SendMessage(chatId, null, replyTo, null, null, contents[0]))
            listOf(messageMapper.mapSingleTdMessage(msg))
        } else {
            val messages = tdLibDataSource.send(
                TdApi.SendMessageAlbum(chatId, null, replyTo, null, contents.toTypedArray())
            )
            val rawMessages = messages.messages.map { messageMapper.mapSingleTdMessage(it) }
            messageMapper.aggregateAlbums(rawMessages)
        }
    }

    override val messageUpdates: Flow<MessageUpdateEvent> = merge(
        tdLibDataSource.newMessageFlow.map {
            MessageUpdateEvent.NewMessage(messageMapper.mapSingleTdMessage(it))
        } as Flow<MessageUpdateEvent>,
        tdLibDataSource.messageContentFlow.transform { update ->
            val msg = tdLibDataSource.sendSafe(TdApi.GetMessage(update.chatId, update.messageId)).getOrNull()
            if (msg != null) emit(MessageUpdateEvent.MessageUpdated(messageMapper.mapSingleTdMessage(msg)))
        },
        tdLibDataSource.messageEditedFlow.transform { update ->
            val msg = tdLibDataSource.sendSafe(TdApi.GetMessage(update.chatId, update.messageId)).getOrNull()
            if (msg != null) emit(MessageUpdateEvent.MessageUpdated(messageMapper.mapSingleTdMessage(msg)))
        },
        tdLibDataSource.messageSendSucceededFlow.transform { update ->
            val msg = tdLibDataSource.sendSafe(TdApi.GetMessage(update.message.chatId, update.message.id)).getOrNull()
            if (msg != null) emit(
                MessageUpdateEvent.MessageSendSucceeded(update.oldMessageId, messageMapper.mapSingleTdMessage(msg))
            )
        },
        tdLibDataSource.updates.filterIsInstance<TdApi.UpdateMessageInteractionInfo>().transform { update ->
            val msg = tdLibDataSource.sendSafe(TdApi.GetMessage(update.chatId, update.messageId)).getOrNull()
            if (msg != null) emit(MessageUpdateEvent.MessageUpdated(messageMapper.mapSingleTdMessage(msg)))
        }
    )

    override suspend fun sendSticker(chatId: Long, sticker: MessageBlock.StickerBlock): Result<Message> = runCatching {
        val fileId = sticker.file.fileId ?: error("Sticker has no fileId")
        val content = TdApi.InputMessageSticker(TdApi.InputFileId(fileId), null, 0, 0, sticker.caption.content)
        val msg = tdLibDataSource.send(TdApi.SendMessage(chatId, null, null, null, null, content))
        messageMapper.mapSingleTdMessage(msg)
    }

    override suspend fun getCustomEmojiStickers(customEmojiIds: List<Long>): Result<List<MessageBlock.StickerBlock>> = runCatching {
        val result = tdLibDataSource.send(TdApi.GetCustomEmojiStickers(customEmojiIds.toLongArray()))
        if (result !is TdApi.Stickers) error("Invalid stickers response type")
        result.stickers.map { sticker ->
            MessageBlock.StickerBlock(
                id = 0,
                timestamp = 0,
                stickerFormat = MessageDto.mapStickerFormat(sticker.format),
                file = com.mocharealm.compound.domain.model.File(fileId = sticker.sticker.id),
                thumbnail = sticker.thumbnail?.file?.id?.let { com.mocharealm.compound.domain.model.File(fileId = it) },
                caption = com.mocharealm.compound.domain.model.Text("")
            )
        }
    }

    override suspend fun sendLocation(chatId: Long, latitude: Double, longitude: Double): Result<Message> = runCatching {
        val location = TdApi.Location(latitude, longitude, 0.0)
        val content = TdApi.InputMessageLocation(location, 0, 0, 0)
        val msg = tdLibDataSource.send(TdApi.SendMessage(chatId, null, null, null, null, content))
        messageMapper.mapSingleTdMessage(msg)
    }

    override suspend fun setChatDraftMessage(chatId: Long, replyToMessageId: Long, draftText: String): Result<Unit> = runCatching {
        val draft = if (draftText.isEmpty()) null else TdApi.DraftMessage().apply {
            this.replyTo = if (replyToMessageId != 0L) TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0) else null
            this.inputMessageText = TdApi.InputMessageText(TdApi.FormattedText(draftText, emptyArray()), null, true)
        }
        tdLibDataSource.send(TdApi.SetChatDraftMessage(chatId, null, draft))
        Result.success(Unit)
    }
}
