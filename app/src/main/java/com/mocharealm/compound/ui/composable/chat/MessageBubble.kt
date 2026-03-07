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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
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
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.screen.chat.GroupPosition
import com.mocharealm.compound.ui.screen.chat.composable.ShareSourceCard
import com.mocharealm.compound.ui.shape.BubbleAlignment
import com.mocharealm.compound.ui.shape.BubbleContinuousShape
import com.mocharealm.compound.ui.util.buildAnnotatedString
import com.mocharealm.compound.ui.util.copyRelativeLightness
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.modifier.surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MessageBubble(
    message: Message,
    groupPosition: GroupPosition,
    showAvatar: Boolean = true,
    onReplyClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val isFirst = groupPosition == GroupPosition.FIRST || groupPosition == GroupPosition.SINGLE
    val isLast = groupPosition == GroupPosition.LAST || groupPosition == GroupPosition.SINGLE
    val hasSticker =
        message.blocks.any { b -> b is MessageBlock.StickerBlock } && message.blocks.size == 1
    
    val hasTextBlock = message.blocks.any { it is MessageBlock.TextBlock }
    val isOnlyAlbum = !hasTextBlock && message.shareInfo == null && message.blocks.all { it is MessageBlock.MediaBlock && it.mediaAlbumId != 0L }

    val topPad = if (isFirst) 8.dp else 2.dp
    val bottomPad = if (isLast) 8.dp else 2.dp

    val rowPadding =
        if (message.isOutgoing) {
            Modifier.padding(start = 64.dp, end = 12.dp, top = topPad, bottom = bottomPad)
        } else {
            Modifier.padding(start = 12.dp, end = 64.dp, top = topPad, bottom = bottomPad)
        }

    Row(
        modifier = modifier.then(rowPadding),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (showAvatar) {
            if (!message.isOutgoing) {
                if (isLast) {
                    Avatar(
                        initials = message.sender.initials,
                        modifier = Modifier.size(36.dp),
                        photoPath = message.sender.profilePhotoUrl
                    )
                } else {
                    Spacer(Modifier.size(36.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        val shape: Shape =
            remember(isLast) {
                if (isLast) {
                    BubbleContinuousShape(
                        if (message.isOutgoing) BubbleAlignment.End else BubbleAlignment.Start,
                        CornerSize(20.dp)
                    )
                } else {
                    ContinuousRoundedRectangle(20.dp)
                }
            }

        Column(
            Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showAvatar) {
                if (!message.isOutgoing && isFirst) {
                    Text(
                        text = message.sender.name.ifEmpty { message.sender.id.toString() },
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

            val colorScheme = MiuixTheme.colorScheme

            val contentColor = remember(message.isOutgoing) {
                if (message.isOutgoing) colorScheme.onPrimary
                else colorScheme.onSurfaceContainer
            }

            CompositionLocalProvider(
                LocalContentColor provides contentColor,
                LocalTextSelectionColors provides TextSelectionColors(
                    handleColor =
                        if (message.isOutgoing)
                            MiuixTheme.colorScheme.primary.copyRelativeLightness(relativeLightness = 0.2f)
                        else
                            MiuixTheme.colorScheme.primary,
                    backgroundColor = contentColor.copy(0.2f)
                )
            ) {
                if (hasSticker || isOnlyAlbum) {
                    MessageContent(message, hasTail = isLast, onReplyClick = onReplyClick)
                } else {
                    Box(
                        modifier =
                            Modifier
                                .surface(
                                    shape = shape,
                                    color =
                                        if (message.isOutgoing)
                                            MiuixTheme.colorScheme.primary
                                        else
                                            MiuixTheme.colorScheme
                                                .surfaceContainer,
                                )
                                .semantics(mergeDescendants = false) {
                                    isTraversalGroup = true
                                },
                        propagateMinConstraints = true,
                    ) {
                        MessageContent(
                            message = message,
                            hasTail = isLast,
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
    onReplyClick: (Long) -> Unit = {},
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val contentColor = LocalContentColor.current
    val linkColor =
        if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
        else MiuixTheme.colorScheme.primary
    val revealedSpoilers = rememberSaveable(message.id) { mutableStateOf(setOf<Int>()) }

    val bottomPadding = if (hasTail) 18.dp else 10.dp
    val hasReply = message.replyTo != null
    val hasShare = message.shareInfo != null

    // Determine if we should use intrinsic width (e.g. for media blocks)
    val useIntrinsicWidth =
        message.blocks.any {
            (it is MessageBlock.MediaBlock && !it.file.fileUrl.isNullOrEmpty())
        }
    val rootModifier =
        if (useIntrinsicWidth) {
            Modifier
                .width(IntrinsicSize.Min)
                .widthIn(44.dp)
        } else {
            Modifier.widthIn(44.dp)
        }

    Column(modifier = rootModifier) {
        if (hasReply) {
            val firstBlock = if (message.blocks.isNotEmpty()) message.blocks[0] else null
            val isOnlySticker = message.blocks.size == 1 && firstBlock is MessageBlock.StickerBlock
            val replyTop = if (isOnlySticker) 0.dp else 10.dp
            val replyBottom = if (message.blocks.isEmpty() && !hasShare) bottomPadding else 0.dp

            val replyText =
                message.replyTo.blocks.find { b -> b is MessageBlock.TextBlock }.let { b ->
                    if (b is MessageBlock.TextBlock) b.content.content else "Media"
                }

            ReplyPreview(
                senderName = message.replyTo.sender.name,
                text = replyText,
                accentColor = linkColor,
                onClick = { onReplyClick(message.replyTo.id) },
                modifier =
                    Modifier
                        .padding(top = replyTop, bottom = replyBottom)
                        .then(
                            if (isOnlySticker) Modifier
                            else Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                        )
            )
        }

        message.blocks.forEachIndexed { index, block ->
            val isFirstBlock = index == 0
            val isLastBlock = index == message.blocks.size - 1
            val blockTop = if (isFirstBlock) (if (hasReply) 8.dp else 10.dp) else 4.dp
            val blockBottom = if (isLastBlock && !hasShare) bottomPadding else 0.dp

            when (block) {
                is MessageBlock.StickerBlock -> {
                    StickerBlock(
                        block = block,
                        modifier =
                            Modifier
                                .padding(top = if (hasReply) 8.dp else 0.dp)
                                .size(120.dp)
                    )
                }

                is MessageBlock.MediaBlock -> {
                    val mediaBlocks =
                        message.blocks.filter { it is MessageBlock.MediaBlock }.map {
                            it as MessageBlock.MediaBlock
                        }
                    val albumId = block.mediaAlbumId
                    if (albumId != 0L) {
                        val isFirstInAlbum =
                            mediaBlocks.find { it.mediaAlbumId == albumId } == block
                        if (isFirstInAlbum) {
                            val albumBlocks = mediaBlocks.filter { it.mediaAlbumId == albumId }
                            val hasTextBlockInside = message.blocks.any { it is MessageBlock.TextBlock }
                            MediaAlbumGrid(albumBlocks, hasTextBlock = hasTextBlockInside, modifier = Modifier.widthIn(max = 280.dp))
                        }
                    } else {
                        if (block.mediaType == MessageBlock.MediaBlock.MediaType.PHOTO) {
                            PhotoBlock(block)
                        } else {
                            VideoBlock(block)
                        }
                    }
                }

                is MessageBlock.DocumentBlock -> {
                    DocumentBlock(
                        block = block,
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = 12.dp,
                                )
                                .padding(
                                    top = blockTop,
                                    bottom = blockBottom
                                )
                    )
                }

                is MessageBlock.TextBlock -> {
                    val richText =
                        remember(
                            block.content.content,
                            block.content.entities,
                            revealedSpoilers.value
                        ) {
                            buildAnnotatedString(
                                block.content.content,
                                block.content.entities,
                                linkColor,
                                revealedSpoilers.value
                            )
                        }
                    RichTextContent(
                        text = richText,
                        contentColor = contentColor,
                        uriHandler = uriHandler,
                        revealedEntityIndices = revealedSpoilers.value,
                        onSpoilerClick = { i -> revealedSpoilers.value += i },
                        modifier =
                            Modifier.padding(
                                top = blockTop,
                                bottom = blockBottom,
                                start = 12.dp,
                                end = 12.dp
                            )
                    )
                }

                is MessageBlock.SystemActionBlock -> {}
                is MessageBlock.VenueBlock -> {
                    VenueBlock(block)
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            val reactionTop = if (hasReply || message.blocks.isNotEmpty() || hasShare) 8.dp else 10.dp
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = reactionTop)
            ) {
                items(message.reactions, key = { "${it.emoji}-${it.isChosen}" }) { reaction ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.surface(
                                shape = CircleShape,
                                color =
                                    if (reaction.isChosen) {
                                        MiuixTheme.colorScheme.primary.copy(alpha = 0.18f)
                                    } else {
                                        MiuixTheme.colorScheme.surfaceContainerHighest
                                    }
                            ).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = reaction.emoji, style = MiuixTheme.textStyles.caption1)
                        Spacer(Modifier.width(4.dp))
                        Text(text = reaction.count.toString(), style = MiuixTheme.textStyles.caption1)
                    }
                }
            }
        }

        if (hasShare) {
            val shareTop = if (hasReply || message.blocks.isNotEmpty()) 8.dp else 10.dp
            ShareSourceCard(
                shareInfo = message.shareInfo,
                modifier =
                    Modifier.padding(
                        top = shareTop,
                        bottom = bottomPadding,
                        start = 12.dp,
                        end = 12.dp
                    ),
                accentColor = contentColor,
            )
        }
    }
}
