package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.ReplyInfo
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.TextEntityType
import com.mocharealm.compound.domain.util.ShareProtocol
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
    val hasSpoiler: Boolean = false,
    val thumbnailFileId: Int? = null,
    val mediaWidth: Int = 0,
    val mediaHeight: Int = 0
) {
    fun toDomain(): Message {
        val shareInfo = ShareProtocol.decode(content, entities)
        if (shareInfo != null) {
            android.util.Log.d("ShareProtocol", "Decoded ShareInfo: $shareInfo")
        } else {
            // Look for ANY compound share URL to see if it exists but decode failed
            val rawUrl =
                entities.find { (it.type as? TextEntityType.TextUrl)?.url?.startsWith("https://compound.mocharealm.com/share") == true }
            if (rawUrl != null) {
                android.util.Log.d(
                    "ShareProtocol",
                    "Found raw URL entity but decode returned null: ${(rawUrl.type as TextEntityType.TextUrl).url}"
                )
            }
        }
        val (strippedContent, strippedEntities) = ShareProtocol.strip(content, entities)
        return Message(
            id = id,
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            content = strippedContent,
            timestamp = timestamp,
            isOutgoing = isOutgoing,
            messageType = messageType,
            fileUrl = null,
            fileId = fileId,
            avatarUrl = avatarUrl,
            stickerFormat = stickerFormat,
            entities = strippedEntities,
            replyTo = replyTo,
            mediaAlbumId = mediaAlbumId,
            hasSpoiler = hasSpoiler,
            thumbnailFileId = thumbnailFileId,
            mediaWidth = mediaWidth,
            mediaHeight = mediaHeight,
            shareInfo = shareInfo
        )
    }

    companion object {
        data class ParsedContent(
            val text: String,
            val type: MessageType,
            val fileId: Int?,
            val stickerFormat: StickerFormat? = null,
            val entities: List<TextEntity> = emptyList(),
            val hasSpoiler: Boolean = false,
            val thumbnailFileId: Int? = null,
            val mediaWidth: Int = 0,
            val mediaHeight: Int = 0
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
                hasSpoiler = parsed.hasSpoiler,
                thumbnailFileId = parsed.thumbnailFileId,
                mediaWidth = parsed.mediaWidth,
                mediaHeight = parsed.mediaHeight
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

                is TdApi.MessageVideo -> {
                    val caption = content.caption.text
                    val videoFileId = content.video.video.id
                    val thumbFileId = content.video.thumbnail?.file?.id
                    ParsedContent(
                        if (caption.isNotEmpty()) "Video: $caption" else "Video",
                        MessageType.VIDEO,
                        videoFileId,
                        hasSpoiler = content.hasSpoiler,
                        thumbnailFileId = thumbFileId,
                        mediaWidth = content.video.width,
                        mediaHeight = content.video.height
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

                is TdApi.MessageAnimation -> {
                    val format = when (content.animation.mimeType) {
                        "image/gif" -> StickerFormat.GIF
                        "video/mp4" -> StickerFormat.MP4
                        else -> StickerFormat.WEBP
                    }
                    ParsedContent(
                        "Sticker",
                        MessageType.STICKER,
                        content.animation.animation.id,
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

                is TdApi.MessageDocument -> {
                    when {
                        content.document.mimeType.startsWith("image/") -> ParsedContent(
                            content.caption.text,
                            MessageType.PHOTO,
                            content.document.document.id,
                            entities = mapFormattedTextEntities(content.caption)
                        )
                        content.document.mimeType.startsWith("video/") -> ParsedContent(
                            content.caption.text,
                            MessageType.VIDEO,
                            content.document.document.id,
                            entities = mapFormattedTextEntities(content.caption)
                        )

                        else -> ParsedContent(
                            content.caption.text,
                            MessageType.DOCUMENT,
                            null,
                            entities = mapFormattedTextEntities(content.caption)
                        )
                    }
                }

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