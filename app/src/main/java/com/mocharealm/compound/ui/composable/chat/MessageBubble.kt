package com.mocharealm.compound.ui.composable.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.screen.chat.GroupPosition
import com.mocharealm.compound.ui.screen.chat.composable.ShareSourceCard
import com.mocharealm.compound.ui.shape.BubbleContinuousShape
import com.mocharealm.compound.ui.shape.BubbleSide
import com.mocharealm.compound.ui.util.buildAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.modifier.surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.collections.plus

@Composable
fun MessageBubble(
    message: Message,
    groupPosition: GroupPosition,
    showAvatar: Boolean = true,
    albumMessages: List<Message>? = null,
    onReplyClick: (Long) -> Unit = {},
) {
    val isFirst = groupPosition == GroupPosition.FIRST || groupPosition == GroupPosition.SINGLE
    val isLast = groupPosition == GroupPosition.LAST || groupPosition == GroupPosition.SINGLE
    val isSticker = message.messageType == MessageType.STICKER

    val topPad = if (isFirst) 8.dp else 2.dp
    val bottomPad = if (isLast) 8.dp else 2.dp

    val rowPadding = if (message.isOutgoing) {
        Modifier.padding(start = 64.dp, end = 12.dp, top = topPad, bottom = bottomPad)
    } else {
        Modifier.padding(start = 12.dp, end = 64.dp, top = topPad, bottom = bottomPad)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowPadding),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (showAvatar) {
            if (!message.isOutgoing) {
                if (isLast) {
                    Avatar(
                        initials = message.senderName.take(2).uppercase(),
                        modifier = Modifier.size(36.dp),
                        photoPath = message.avatarUrl
                    )
                } else {
                    Spacer(Modifier.size(36.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        val shape: Shape = remember(isLast) {
            if (isLast) {
                BubbleContinuousShape(
                    if (message.isOutgoing) BubbleSide.Right else BubbleSide.Left, CornerSize(20.dp)
                )
            } else {
                ContinuousRoundedRectangle(20.dp)
            }
        }

        Column(
            Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showAvatar) {
                if (!message.isOutgoing && isFirst) {
                    Text(
                        text = message.senderName.ifBlank { message.senderId.toString() },
                        fontWeight = FontWeight.SemiBold,
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .alpha(0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            CompositionLocalProvider(
                LocalContentColor provides if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
                else MiuixTheme.colorScheme.onSurfaceContainer,
            ) {
                if (isSticker) {
                    MessageContent(
                        message, hasTail = isLast, albumMessages = null, onReplyClick = onReplyClick
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .surface(
                                shape = shape,
                                color = if (message.isOutgoing) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.surfaceContainer,
                            )
                            .semantics(mergeDescendants = false) {
                                isTraversalGroup = true
                            },
                        propagateMinConstraints = true,
                    ) {
                        MessageContent(
                            message = message,
                            hasTail = isLast,
                            albumMessages = albumMessages,
                            onReplyClick = onReplyClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageContent(
    message: Message,
    hasTail: Boolean,
    albumMessages: List<Message>? = null,
    onReplyClick: (Long) -> Unit = {},
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val contentColor = LocalContentColor.current
    val linkColor =
        if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.primary
    val revealedSpoilers = rememberSaveable(message.id) { mutableStateOf(setOf<Int>()) }

    val bottomPadding = if (hasTail) 18.dp else 10.dp

    val hasReply = message.replyTo != null
    val isSticker = message.messageType == MessageType.STICKER
    val isAlbum = albumMessages != null && albumMessages.size > 1
    val isPhoto = message.messageType == MessageType.PHOTO && !isAlbum
    val isVideo = message.messageType == MessageType.VIDEO && !isAlbum
    val hasMedia = isPhoto || isVideo || isAlbum
    val isDocument = message.messageType == MessageType.DOCUMENT
    val hasShare = message.shareInfo != null

    val textStr = when {
        isAlbum -> albumMessages.firstNotNullOfOrNull {
            it.content.removePrefix("Photo: ").removePrefix("Video: ")
                .takeIf { s -> s != "Photo" && s != "Video" && s.isNotBlank() }
        } ?: ""

        isPhoto -> message.content.removePrefix("Photo: ").takeIf { it != "Photo" } ?: ""
        isVideo -> message.content.removePrefix("Video: ").takeIf { it != "Video" } ?: ""
        !isSticker -> message.content
        else -> ""
    }
    val hasText = textStr.isNotBlank()

    val useIntrinsicWidth = (isPhoto && !message.fileUrl.isNullOrEmpty()) || isVideo
    val rootModifier = if (useIntrinsicWidth) {
        Modifier
            .width(IntrinsicSize.Min)
            .widthIn(44.dp)
    } else {
        Modifier.widthIn(44.dp)
    }

    Column(modifier = rootModifier) {
        if (hasReply) {
            val replyTop = if (isSticker) 0.dp else 10.dp
            val replyBottom =
                if (isSticker) 0.dp else if (!hasMedia && !hasText && !hasShare && !isDocument) bottomPadding else 0.dp
            ReplyPreview(
                senderName = message.replyTo.senderName,
                text = message.replyTo.text,
                accentColor = linkColor,
                onClick = { onReplyClick(message.replyTo.messageId) },
                modifier = Modifier
                    .padding(top = replyTop, bottom = replyBottom)
                    .then(if (isSticker) Modifier else Modifier.padding(horizontal = 12.dp))
            )
        }

        if (isSticker) {
            StickerBlock(
                message = message,
                modifier = Modifier
                    .padding(top = if (hasReply) 8.dp else 0.dp)
                    .size(120.dp)
            )
        } else if (hasMedia) {
            Box(modifier = Modifier.padding(top = if (hasReply) 8.dp else 0.dp)) {
                if (isAlbum) MediaAlbumGrid(
                    albumMessages, modifier = Modifier.widthIn(max = 280.dp)
                )
                else if (isPhoto) PhotoBlock(message)
                else if (isVideo) VideoBlock(message)
            }
        } else if (isDocument) {
            val fileTop = if (hasReply) 8.dp else 10.dp
            val fileBottom = if (!hasText && !hasShare) bottomPadding else 0.dp
            Text(
                text = "File: ${message.content}",
                color = contentColor,
                modifier = Modifier.padding(
                    top = fileTop, bottom = fileBottom, start = 12.dp, end = 12.dp
                )
            )
        }

        if (hasText && !isSticker) {
            val textTop = if (hasReply || hasMedia || isDocument) 8.dp else 10.dp
            val textBottom = if (hasShare) 0.dp else bottomPadding
            val richText = remember(textStr, message.entities, revealedSpoilers.value) {
                buildAnnotatedString(textStr, message.entities, linkColor, revealedSpoilers.value)
            }
            RichTextContent(
                text = richText,
                contentColor = contentColor,
                uriHandler = uriHandler,
                revealedEntityIndices = revealedSpoilers.value,
                onSpoilerClick = { index -> revealedSpoilers.value += index },
                // 去掉了原本错误赋予的 fillMaxWidth()
                modifier = Modifier.padding(
                    top = textTop, bottom = textBottom, start = 12.dp, end = 12.dp
                )
            )
        }

        if (hasShare) {
            val shareTop = if (hasReply || hasMedia || isDocument || hasText) 8.dp else 10.dp
            ShareSourceCard(
                shareInfo = message.shareInfo,
                modifier = Modifier.padding(
                    top = shareTop, bottom = bottomPadding, start = 12.dp, end = 12.dp
                ),
                accentColor = contentColor,
            )
        }
    }
}