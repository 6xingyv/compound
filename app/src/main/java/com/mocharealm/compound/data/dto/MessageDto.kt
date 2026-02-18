package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.ReplyInfo
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.TextEntityType
import org.drinkless.tdlib.TdApi

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
    val stickerFormat: StickerFormat? = null,
    val entities: List<TextEntity> = emptyList(),
    val replyTo: ReplyInfo? = null,
    val mediaAlbumId: Long = 0L,
    val hasSpoiler: Boolean = false
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
        stickerFormat = stickerFormat,
        entities = entities,
        replyTo = replyTo,
        mediaAlbumId = mediaAlbumId,
        hasSpoiler = hasSpoiler
    )

    companion object {
        data class ParsedContent(
            val text: String,
            val type: MessageType,
            val fileId: Int?,
            val stickerFormat: StickerFormat? = null,
            val entities: List<TextEntity> = emptyList(),
            val hasSpoiler: Boolean = false
        )

        fun fromTdApi(
            msg: TdApi.Message,
            senderId: Long,
            senderName: String,
            avatarPath: String?,
            replyInfo: ReplyInfo?
        ): MessageDto {
            val parsed = parseMessageContent(msg.content)
            return MessageDto(
                id = msg.id,
                chatId = msg.chatId,
                senderId = senderId,
                senderName = senderName,
                content = parsed.text,
                timestamp = msg.date.toLong(),
                isOutgoing = msg.isOutgoing,
                messageType = parsed.type,
                fileId = parsed.fileId,
                avatarUrl = avatarPath,
                stickerFormat = parsed.stickerFormat,
                entities = parsed.entities,
                replyTo = replyInfo,
                mediaAlbumId = msg.mediaAlbumId,
                hasSpoiler = parsed.hasSpoiler
            )
        }

        fun parseMessageContent(content: TdApi.MessageContent): ParsedContent =
            when (content) {
                is TdApi.MessageText -> ParsedContent(
                    content.text.text,
                    MessageType.TEXT,
                    null,
                    entities = mapFormattedTextEntities(content.text)
                )

                is TdApi.MessagePhoto -> {
                    val caption = content.caption.text
                    val photoFileId = content.photo.sizes.lastOrNull()?.photo?.id
                    ParsedContent(
                        if (caption.isNotEmpty()) "Photo: $caption" else "Photo",
                        MessageType.PHOTO,
                        photoFileId,
                        entities = if (caption.isNotEmpty()) mapFormattedTextEntities(content.caption) else emptyList(),
                        hasSpoiler = content.hasSpoiler
                    )
                }

                is TdApi.MessageAnimatedEmoji -> {
                    val sticker = content.animatedEmoji.sticker
                    val format = when (sticker?.format) {
                        is TdApi.StickerFormatWebp -> StickerFormat.WEBP
                        is TdApi.StickerFormatTgs -> StickerFormat.TGS
                        is TdApi.StickerFormatWebm -> StickerFormat.WEBM
                        else -> StickerFormat.WEBP
                    }
                    ParsedContent(
                        content.emoji,
                        MessageType.STICKER,
                        sticker?.sticker?.id,
                        format
                    )
                }

                is TdApi.MessageSticker -> {
                    val format = when (content.sticker.format) {
                        is TdApi.StickerFormatWebp -> StickerFormat.WEBP
                        is TdApi.StickerFormatTgs -> StickerFormat.TGS
                        is TdApi.StickerFormatWebm -> StickerFormat.WEBM
                        else -> StickerFormat.WEBP
                    }
                    ParsedContent(
                        "Sticker",
                        MessageType.STICKER,
                        content.sticker.sticker.id,
                        format
                    )
                }

                is TdApi.MessageVideo -> {
                    val caption = content.caption.text
                    ParsedContent(
                        if (caption.isNotEmpty()) "Video: $caption" else "Video",
                        MessageType.VIDEO,
                        null,
                        hasSpoiler = content.hasSpoiler
                    )
                }

                is TdApi.MessageDocument -> ParsedContent(
                    "Document: ${content.document.fileName}",
                    MessageType.DOCUMENT,
                    null
                )

                is TdApi.MessageAudio -> ParsedContent("Audio", MessageType.AUDIO, null)
                is TdApi.MessageVoiceNote -> ParsedContent("Voice message", MessageType.VOICE, null)
                is TdApi.MessageChatAddMembers,
                is TdApi.MessageChatJoinByLink,
                is TdApi.MessageChatDeleteMember,
                is TdApi.MessageChatChangeTitle,
                is TdApi.MessageChatChangePhoto,
                is TdApi.MessagePinMessage,
                is TdApi.MessageChatUpgradeTo -> ParsedContent(
                    "System message",
                    MessageType.SYSTEM,
                    null
                )

                else -> ParsedContent("Message $content", MessageType.TEXT, null)
            }

        fun mapFormattedTextEntities(formattedText: TdApi.FormattedText): List<TextEntity> {
            if (formattedText.entities.isNullOrEmpty()) return emptyList()
            return formattedText.entities.mapNotNull { entity ->
                val type = when (entity.type) {
                    is TdApi.TextEntityTypeBold -> TextEntityType.Bold
                    is TdApi.TextEntityTypeItalic -> TextEntityType.Italic
                    is TdApi.TextEntityTypeUnderline -> TextEntityType.Underline
                    is TdApi.TextEntityTypeStrikethrough -> TextEntityType.Strikethrough
                    is TdApi.TextEntityTypeCode -> TextEntityType.Code
                    is TdApi.TextEntityTypePre -> TextEntityType.Pre
                    is TdApi.TextEntityTypePreCode -> TextEntityType.PreCode(
                        (entity.type as TdApi.TextEntityTypePreCode).language
                    )

                    is TdApi.TextEntityTypeTextUrl -> TextEntityType.TextUrl(
                        (entity.type as TdApi.TextEntityTypeTextUrl).url
                    )

                    is TdApi.TextEntityTypeUrl -> TextEntityType.Url
                    is TdApi.TextEntityTypeMention -> TextEntityType.Mention
                    is TdApi.TextEntityTypeMentionName -> TextEntityType.Mention
                    is TdApi.TextEntityTypeSpoiler -> TextEntityType.Spoiler
                    is TdApi.TextEntityTypeEmailAddress -> TextEntityType.EmailAddress
                    is TdApi.TextEntityTypePhoneNumber -> TextEntityType.PhoneNumber
                    else -> null
                } ?: return@mapNotNull null
                TextEntity(offset = entity.offset, length = entity.length, type = type)
            }
        }

        fun mapToTdApiEntities(entities: List<TextEntity>): Array<TdApi.TextEntity> {
            return entities.map { entity ->
                val type = when (entity.type) {
                    is TextEntityType.Bold -> TdApi.TextEntityTypeBold()
                    is TextEntityType.Italic -> TdApi.TextEntityTypeItalic()
                    is TextEntityType.Underline -> TdApi.TextEntityTypeUnderline()
                    is TextEntityType.Strikethrough -> TdApi.TextEntityTypeStrikethrough()
                    is TextEntityType.Code -> TdApi.TextEntityTypeCode()
                    is TextEntityType.Pre -> TdApi.TextEntityTypePre()
                    is TextEntityType.PreCode -> TdApi.TextEntityTypePreCode(entity.type.language)
                    is TextEntityType.TextUrl -> TdApi.TextEntityTypeTextUrl(entity.type.url)
                    is TextEntityType.Url -> TdApi.TextEntityTypeUrl()
                    is TextEntityType.Mention -> TdApi.TextEntityTypeMention()
                    is TextEntityType.Spoiler -> TdApi.TextEntityTypeSpoiler()
                    is TextEntityType.EmailAddress -> TdApi.TextEntityTypeEmailAddress()
                    is TextEntityType.PhoneNumber -> TdApi.TextEntityTypePhoneNumber()
                }
                TdApi.TextEntity(entity.offset, entity.length, type)
            }.toTypedArray()
        }
    }
}