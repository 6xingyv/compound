package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Document
import com.mocharealm.compound.domain.model.File
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.model.Venue
import org.drinkless.tdlib.TdApi

/**
 * Stateless helpers that convert TDLib API objects into domain [MessageBlock] instances.
 *
 * Album aggregation (merging multiple TdApi.Messages into one domain Message) is performed in the
 * repository layer, not here.
 */
object MessageDto {
        fun parseMessageContent(
                content: TdApi.MessageContent,
                messageId: Long,
                timestamp: Long,
                mediaAlbumId: Long = 0L,
        ): List<MessageBlock> {
                val captionBlock: (TdApi.FormattedText) -> MessageBlock.TextBlock? = { fmt ->
                        mapFormattedTextOrNull(fmt)?.let { text ->
                                MessageBlock.TextBlock(
                                        id = messageId,
                                        timestamp = timestamp,
                                        content = text
                                )
                        }
                }

                return when (content) {
                        is TdApi.MessageText ->
                                listOf(
                                        MessageBlock.TextBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                content = mapFormattedText(content.text),
                                        )
                                )
                        is TdApi.MessagePhoto -> {
                                val largestSize = content.photo.sizes.maxByOrNull { it.width * it.height }
                                val photoFileId = largestSize?.photo?.id
                                val media =
                                        MessageBlock.MediaBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                mediaType = MessageBlock.MediaBlock.MediaType.PHOTO,
                                                file = File(fileId = photoFileId),
                                                width = largestSize?.width ?: 0,
                                                height = largestSize?.height ?: 0,
                                                hasSpoiler = content.hasSpoiler,
                                                mediaAlbumId = mediaAlbumId,
                                        )
                                listOfNotNull(media, captionBlock(content.caption))
                        }
                        is TdApi.MessageVideo -> {
                                val videoFileId = content.video.video.id
                                val thumbFileId = content.video.thumbnail?.file?.id
                                val media =
                                        MessageBlock.MediaBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                mediaType = MessageBlock.MediaBlock.MediaType.VIDEO,
                                                file = File(fileId = videoFileId),
                                                thumbnail = thumbFileId?.let { File(fileId = it) },
                                                hasSpoiler = content.hasSpoiler,
                                                width = content.video.width,
                                                height = content.video.height,
                                                mediaAlbumId = mediaAlbumId,
                                        )
                                listOfNotNull(media, captionBlock(content.caption))
                        }
                        is TdApi.MessageAnimatedEmoji -> {
                                val sticker = content.animatedEmoji.sticker
                                val format = mapStickerFormat(sticker?.format)
                                listOf(
                                        MessageBlock.StickerBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                stickerFormat = format,
                                                file = File(fileId = sticker?.sticker?.id),
                                                caption = Text(content.emoji),
                                        )
                                )
                        }
                        is TdApi.MessageAnimation -> {
                                val format =
                                        when (content.animation.mimeType) {
                                                "image/gif" ->
                                                        MessageBlock.StickerBlock.StickerFormat.GIF
                                                "video/mp4" ->
                                                        MessageBlock.StickerBlock.StickerFormat.MP4
                                                else -> MessageBlock.StickerBlock.StickerFormat.WEBP
                                        }
                                listOf(
                                        MessageBlock.StickerBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                stickerFormat = format,
                                                file =
                                                        File(
                                                                fileId =
                                                                        content.animation
                                                                                .animation
                                                                                .id
                                                        ),
                                                caption = Text("Sticker"),
                                        )
                                )
                        }
                        is TdApi.MessageSticker -> {
                                val format = mapStickerFormat(content.sticker.format)
                                listOf(
                                        MessageBlock.StickerBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                stickerFormat = format,
                                                file = File(fileId = content.sticker.sticker.id),
                                                caption = Text("Sticker"),
                                        )
                                )
                        }
                        is TdApi.MessageDocument -> {
                                when {
                                        content.document.mimeType.startsWith("image/") -> {
                                                val media =
                                                        MessageBlock.MediaBlock(
                                                                id = messageId,
                                                                timestamp = timestamp,
                                                                mediaType =
                                                                        MessageBlock.MediaBlock
                                                                                .MediaType.PHOTO,
                                                                file =
                                                                        File(
                                                                                fileId =
                                                                                        content.document
                                                                                                .document
                                                                                                .id
                                                                        ),
                                                                mediaAlbumId = mediaAlbumId,
                                                        )
                                                listOfNotNull(media, captionBlock(content.caption))
                                        }
                                        content.document.mimeType.startsWith("video/") -> {
                                                val media =
                                                        MessageBlock.MediaBlock(
                                                                id = messageId,
                                                                timestamp = timestamp,
                                                                mediaType =
                                                                        MessageBlock.MediaBlock
                                                                                .MediaType.VIDEO,
                                                                file =
                                                                        File(
                                                                                fileId =
                                                                                        content.document
                                                                                                .document
                                                                                                .id
                                                                        ),
                                                                mediaAlbumId = mediaAlbumId,
                                                        )
                                                listOfNotNull(media, captionBlock(content.caption))
                                        }
                                        else -> {
                                                val thumbFileId =
                                                        content.document.thumbnail?.file?.id
                                                val doc =
                                                        MessageBlock.DocumentBlock(
                                                                id = messageId,
                                                                timestamp = timestamp,
                                                                document =
                                                                        Document(
                                                                                file =
                                                                                        File(
                                                                                                fileId =
                                                                                                        content.document
                                                                                                                .document
                                                                                                                .id
                                                                                        ),
                                                                                fileName =
                                                                                        content.document
                                                                                                .fileName,
                                                                                mimeType =
                                                                                        content.document
                                                                                                .mimeType,
                                                                                thumbnail =
                                                                                        thumbFileId
                                                                                                ?.let {
                                                                                                        File(
                                                                                                                fileId =
                                                                                                                        it
                                                                                                        )
                                                                                                },
                                                                        ),
                                                                mediaAlbumId = mediaAlbumId,
                                                        )
                                                listOfNotNull(doc, captionBlock(content.caption))
                                        }
                                }
                        }
                        is TdApi.MessageAudio ->
                                listOf(
                                        MessageBlock.TextBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                content = Text("Audio"),
                                        )
                                )
                        is TdApi.MessageVoiceNote ->
                                listOf(
                                        MessageBlock.TextBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                content = Text("Voice message"),
                                        )
                                )

                        // System messages – handled by the repo layer with SystemActionBlock.
                        is TdApi.MessageChatAddMembers,
                        is TdApi.MessageChatJoinByLink,
                        is TdApi.MessageChatDeleteMember,
                        is TdApi.MessageChatChangeTitle,
                        is TdApi.MessageChatChangePhoto,
                        is TdApi.MessagePinMessage,
                        is TdApi.MessageChatUpgradeTo ->
                                listOf(
                                        MessageBlock.TextBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                content = Text("System message"),
                                        )
                                )
                        is TdApi.MessageVenue ->
                                listOf(
                                        MessageBlock.VenueBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                venue =
                                                        Venue(
                                                                longitude =
                                                                        content.venue
                                                                                .location
                                                                                .longitude,
                                                                latitude =
                                                                        content.venue
                                                                                .location
                                                                                .latitude,
                                                                name = content.venue.title,
                                                        ),
                                        )
                                )
                        else ->
                                listOf(
                                        MessageBlock.TextBlock(
                                                id = messageId,
                                                timestamp = timestamp,
                                                content = Text("Message $content"),
                                        )
                                )
                }
        }

        fun mapFormattedText(formattedText: TdApi.FormattedText): Text {
                return Text(
                        content = formattedText.text,
                        entities = mapFormattedTextEntities(formattedText),
                )
        }

        private fun mapFormattedTextOrNull(formattedText: TdApi.FormattedText): Text? {
                if (formattedText.text.isBlank() && formattedText.entities.isNullOrEmpty())
                        return null
                return mapFormattedText(formattedText)
        }

        fun mapFormattedTextEntities(formattedText: TdApi.FormattedText): List<Text.TextEntity> {
                if (formattedText.entities.isNullOrEmpty()) return emptyList()
                return formattedText.entities.mapNotNull { entity ->
                        val type =
                                when (entity.type) {
                                        is TdApi.TextEntityTypeBold -> Text.TextEntityType.Bold
                                        is TdApi.TextEntityTypeItalic -> Text.TextEntityType.Italic
                                        is TdApi.TextEntityTypeUnderline ->
                                                Text.TextEntityType.Underline
                                        is TdApi.TextEntityTypeStrikethrough ->
                                                Text.TextEntityType.Strikethrough
                                        is TdApi.TextEntityTypeCode -> Text.TextEntityType.Code
                                        is TdApi.TextEntityTypePre -> Text.TextEntityType.Pre
                                        is TdApi.TextEntityTypePreCode ->
                                                Text.TextEntityType.PreCode(
                                                        (entity.type as TdApi.TextEntityTypePreCode)
                                                                .language
                                                )
                                        is TdApi.TextEntityTypeTextUrl ->
                                                Text.TextEntityType.TextUrl(
                                                        (entity.type as TdApi.TextEntityTypeTextUrl)
                                                                .url
                                                )
                                        is TdApi.TextEntityTypeUrl -> Text.TextEntityType.Url
                                        is TdApi.TextEntityTypeMention ->
                                                Text.TextEntityType.Mention
                                        is TdApi.TextEntityTypeMentionName ->
                                                Text.TextEntityType.Mention
                                        is TdApi.TextEntityTypeSpoiler ->
                                                Text.TextEntityType.Spoiler
                                        is TdApi.TextEntityTypeEmailAddress ->
                                                Text.TextEntityType.EmailAddress
                                        is TdApi.TextEntityTypePhoneNumber ->
                                                Text.TextEntityType.PhoneNumber
                                        else -> null
                                }
                                        ?: return@mapNotNull null
                        Text.TextEntity(offset = entity.offset, length = entity.length, type = type)
                }
        }

        fun mapToTdApiEntities(entities: List<Text.TextEntity>): Array<TdApi.TextEntity> {
                return entities
                        .map { entity ->
                                val type =
                                        when (entity.type) {
                                                is Text.TextEntityType.Bold ->
                                                        TdApi.TextEntityTypeBold()
                                                is Text.TextEntityType.Italic ->
                                                        TdApi.TextEntityTypeItalic()
                                                is Text.TextEntityType.Underline ->
                                                        TdApi.TextEntityTypeUnderline()
                                                is Text.TextEntityType.Strikethrough ->
                                                        TdApi.TextEntityTypeStrikethrough()
                                                is Text.TextEntityType.Code ->
                                                        TdApi.TextEntityTypeCode()
                                                is Text.TextEntityType.Pre ->
                                                        TdApi.TextEntityTypePre()
                                                is Text.TextEntityType.PreCode ->
                                                        TdApi.TextEntityTypePreCode(
                                                                entity.type.language
                                                        )
                                                is Text.TextEntityType.TextUrl ->
                                                        TdApi.TextEntityTypeTextUrl(entity.type.url)
                                                is Text.TextEntityType.Url ->
                                                        TdApi.TextEntityTypeUrl()
                                                is Text.TextEntityType.Mention ->
                                                        TdApi.TextEntityTypeMention()
                                                is Text.TextEntityType.Spoiler ->
                                                        TdApi.TextEntityTypeSpoiler()
                                                is Text.TextEntityType.EmailAddress ->
                                                        TdApi.TextEntityTypeEmailAddress()
                                                is Text.TextEntityType.PhoneNumber ->
                                                        TdApi.TextEntityTypePhoneNumber()
                                        }
                                TdApi.TextEntity(entity.offset, entity.length, type)
                        }
                        .toTypedArray()
        }

        fun mapStickerFormat(
                format: TdApi.StickerFormat?
        ): MessageBlock.StickerBlock.StickerFormat =
                when (format) {
                        is TdApi.StickerFormatWebp -> MessageBlock.StickerBlock.StickerFormat.WEBP
                        is TdApi.StickerFormatTgs -> MessageBlock.StickerBlock.StickerFormat.TGS
                        is TdApi.StickerFormatWebm -> MessageBlock.StickerBlock.StickerFormat.WEBM
                        else -> MessageBlock.StickerBlock.StickerFormat.WEBP
                }
}
