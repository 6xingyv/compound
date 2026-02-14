package com.mocharealm.compound.ui.screen.chat

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.composable.BackNavigationIcon
import com.mocharealm.compound.ui.shape.BubbleContinuousShape
import com.mocharealm.compound.ui.shape.BubbleSide
import com.mocharealm.compound.ui.util.URL_ANNOTATION_TAG
import com.mocharealm.compound.ui.util.buildAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

private enum class GroupPosition { FIRST, MIDDLE, LAST, SINGLE }

private data class DisplayItem(
    val messages: List<Message>,
    val isAlbum: Boolean
)

@Composable
fun ChatScreen(
    chatId: Long,
    chatTitle: String,
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val listState = rememberLazyListState()

    LaunchedEffect(chatId) {
        viewModel.loadMessages(chatId)
    }

    // 当滚动到顶部（reverseLayout 下即列表末尾）时，加载更多旧消息
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.loading && state.hasMore) {
            viewModel.loadOlderMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SmallTopAppBar(
                title = chatTitle,
                navigationIcon = {
                    BackNavigationIcon(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = { navigator.pop() },
                    )
                },
            )
        },
        popupHost = {},
    ) { innerPadding ->
        val displayMessages = state.messages.reversed()

        // Pre-group album messages: find runs of consecutive messages
        // with the same non-zero mediaAlbumId
        val displayItems = remember(displayMessages) {
            val items = mutableListOf<DisplayItem>()
            var i = 0
            while (i < displayMessages.size) {
                val msg = displayMessages[i]
                val isMediaAlbumType = msg.messageType == MessageType.PHOTO ||
                    msg.messageType == MessageType.VIDEO
                if (msg.mediaAlbumId != 0L && isMediaAlbumType) {
                    val albumId = msg.mediaAlbumId
                    val albumMessages = mutableListOf(msg)
                    while (i + 1 < displayMessages.size &&
                        displayMessages[i + 1].mediaAlbumId == albumId &&
                        (displayMessages[i + 1].messageType == MessageType.PHOTO ||
                            displayMessages[i + 1].messageType == MessageType.VIDEO)
                    ) {
                        i++
                        albumMessages.add(displayMessages[i])
                    }
                    items.add(DisplayItem(albumMessages, isAlbum = true))
                } else {
                    items.add(DisplayItem(listOf(msg), isAlbum = false))
                }
                i++
            }
            items
        }

        val scope = rememberCoroutineScope()
        val onReplyClick: (Long) -> Unit = { replyMessageId ->
            viewModel.scrollToMessage(replyMessageId)
        }

        // Observe scrollToMessageId and scroll to it when it arrives
        val scrollTarget = state.scrollToMessageId
        LaunchedEffect(scrollTarget, displayItems) {
            if (scrollTarget != null) {
                val targetIdx = displayItems.indexOfFirst { item ->
                    item.messages.any { it.id == scrollTarget }
                }
                if (targetIdx >= 0) {
                    listState.animateScrollToItem(targetIdx)
                    viewModel.clearScrollTarget()
                }
            }
        }

        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .scrollEndHaptic(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 12.dp,
            ),
            overscrollEffect = null,
        ) {
            if (state.loading && state.messages.isEmpty()) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        BasicComponent(title = "Loading messages...")
                    }
                }
            } else if (state.error != null && state.messages.isEmpty()) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        BasicComponent(
                            title = "Error",
                            summary = state.error,
                        )
                        TextButton(
                            text = "Retry",
                            onClick = { viewModel.loadMessages(chatId) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else if (state.messages.isEmpty() && state.initialLoaded) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        BasicComponent(title = "No messages in this chat")
                    }
                }
            } else {
                items(displayItems.size, key = { displayItems[it].messages.first().id }) { index ->
                    val item = displayItems[index]
                    val primaryMessage = item.messages.first()

                    // Group position based on the primary message's senderId
                    val prevSender = displayItems.getOrNull(index - 1)?.messages?.first()?.senderId
                    val nextSender = displayItems.getOrNull(index + 1)?.messages?.first()?.senderId
                    val sameBelow = prevSender == primaryMessage.senderId
                    val sameAbove = nextSender == primaryMessage.senderId
                    val groupPosition = when {
                        !sameAbove && !sameBelow -> GroupPosition.SINGLE
                        !sameAbove -> GroupPosition.FIRST
                        !sameBelow -> GroupPosition.LAST
                        else -> GroupPosition.MIDDLE
                    }

                    if (item.isAlbum) {
                        MessageBubble(
                            message = primaryMessage,
                            groupPosition = groupPosition,
                            albumMessages = item.messages,
                            onReplyClick = onReplyClick,
                        )
                    } else {
                        MessageBubble(
                            message = primaryMessage,
                            groupPosition = groupPosition,
                            onReplyClick = onReplyClick,
                        )
                    }
                }

                if (state.loadingMore) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Loading more...",
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    groupPosition: GroupPosition,
    albumMessages: List<Message>? = null,
    onReplyClick: (Long) -> Unit = {},
) {
    val isFirst = groupPosition == GroupPosition.FIRST || groupPosition == GroupPosition.SINGLE
    val isLast = groupPosition == GroupPosition.LAST || groupPosition == GroupPosition.SINGLE

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

        val shape: Shape = remember(isLast) {
            if (isLast) {
                BubbleContinuousShape(
                    if (message.isOutgoing) BubbleSide.Right else BubbleSide.Left,
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
            // 发送者名字仅在第一条 / 单条消息时显示
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
            val isSticker = message.messageType == MessageType.STICKER
            CompositionLocalProvider(
                LocalContentColor provides
                        if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
                        else MiuixTheme.colorScheme.onSurfaceContainer,
            ) {
                if (isSticker) {
                    // 贴纸：透明背景，不使用 shape clip
                    MessageContent(message, hasTail = isLast, onReplyClick = onReplyClick)
                } else {
                    Box(
                        modifier = Modifier
                            .surface(
                                shape = shape,
                                backgroundColor =
                                    if (message.isOutgoing) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.surfaceContainer,
                                border = null,
                                shadowElevation = 0f,
                            )
                            .semantics(mergeDescendants = false) {
                                isTraversalGroup = true
                            },
                        propagateMinConstraints = true,
                    ) {
                        Column(Modifier.widthIn(min = 44.dp)) {
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
}

@Stable
private fun Modifier.surface(
    shape: Shape,
    backgroundColor: Color,
    border: BorderStroke?,
    shadowElevation: Float,
) = this.then(
    if (shadowElevation > 0f) {
        Modifier.graphicsLayer(
            shadowElevation = shadowElevation,
            shape = shape,
            clip = false,
        )
    } else {
        Modifier
    },
)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(color = backgroundColor, shape = shape)
    .clip(shape)

@Composable
private fun MessageContent(
    message: Message,
    hasTail: Boolean,
    albumMessages: List<Message>? = null,
    onReplyClick: (Long) -> Unit = {},
) {
    val textPadding = if (hasTail) {
        Modifier
            .padding(top = 10.dp, bottom = 18.dp)
            .padding(horizontal = 12.dp)
    } else {
        Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val contentColor = LocalContentColor.current
    val linkColor = if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
        else MiuixTheme.colorScheme.primary

    val richText = remember(message.content, message.entities) {
        buildAnnotatedString(message.content, message.entities, linkColor)
    }

    // Reply preview
    if (message.replyTo != null) {
        ReplyPreview(
            senderName = message.replyTo.senderName,
            text = message.replyTo.text,
            accentColor = if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
                else MiuixTheme.colorScheme.primary,
            onClick = { onReplyClick(message.replyTo.messageId) },
        )
    }

    // Media album
    if (albumMessages != null && albumMessages.size > 1) {
        MediaAlbumGrid(albumMessages)
        // Caption from first message with text content
        val caption = albumMessages.firstNotNullOfOrNull { msg ->
            msg.content.removePrefix("Photo: ").removePrefix("Video: ")
                .takeIf { it != "Photo" && it != "Video" && it.isNotBlank() }
        }
        if (caption != null) {
            val captionRich = remember(caption) {
                buildAnnotatedString(caption, message.entities, linkColor)
            }
            RichTextContent(captionRich, contentColor, textPadding, uriHandler)
        }
        return
    }

    when (message.messageType) {
        MessageType.PHOTO -> {
            if (!message.fileUrl.isNullOrEmpty()) {
                Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(java.io.File(message.fileUrl))
                            .build(),
                        contentDescription = "Photo",
                        modifier = Modifier
                            .heightIn(max = 300.dp)
                            .wrapContentWidth(),
                        contentScale = ContentScale.Fit
                    )

                    val caption = message.content.removePrefix("Photo: ").takeIf { it != "Photo" }
                    if (!caption.isNullOrBlank()) {
                        val captionRich = remember(caption, message.entities) {
                            buildAnnotatedString(caption, message.entities, linkColor)
                        }
                        androidx.compose.foundation.text.ClickableText(
                            text = captionRich,
                            style = MiuixTheme.textStyles.body1.copy(color = contentColor),
                            modifier = textPadding.fillMaxWidth(),
                            onClick = { offset ->
                                captionRich.getStringAnnotations(URL_ANNOTATION_TAG, offset, offset)
                                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
                            }
                        )
                    }
                }
            } else {
                RichTextContent(richText, contentColor, textPadding, uriHandler)
            }
        }

        MessageType.STICKER -> {
            if (!message.fileUrl.isNullOrEmpty()) {
                when (message.stickerFormat) {
                    StickerFormat.WEBM -> LoopingVideoSticker(
                        filePath = message.fileUrl,
                        modifier = Modifier.size(120.dp)
                    )
                    StickerFormat.TGS -> LottieSticker(
                        filePath = message.fileUrl,
                        modifier = Modifier.size(120.dp)
                    )
                    else -> AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(java.io.File(message.fileUrl))
                            .build(),
                        contentDescription = "Sticker",
                        modifier = Modifier.size(120.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                RichTextContent(richText, contentColor, textPadding, uriHandler)
            }
        }

        else -> RichTextContent(richText, contentColor, textPadding, uriHandler)
    }
}

@Composable
private fun RichTextContent(
    text: androidx.compose.ui.text.AnnotatedString,
    contentColor: Color,
    modifier: Modifier,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) {
    if (text.spanStyles.isEmpty() && text.getStringAnnotations(0, text.length).isEmpty()) {
        // 无富文本标注，使用普通 Text 组件
        Text(
            text = text.text,
            style = MiuixTheme.textStyles.body1,
            modifier = modifier
        )
    } else {
        androidx.compose.foundation.text.ClickableText(
            text = text,
            style = MiuixTheme.textStyles.body1.copy(color = contentColor),
            modifier = modifier,
            onClick = { offset ->
                text.getStringAnnotations(URL_ANNOTATION_TAG, offset, offset)
                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
            }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun LoopingVideoSticker(filePath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(java.io.File(filePath))))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(filePath) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun LottieSticker(filePath: String, modifier: Modifier = Modifier) {
    val jsonString = remember(filePath) {
        try {
            java.util.zip.GZIPInputStream(java.io.File(filePath).inputStream()).bufferedReader()
                .use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    if (jsonString != null) {
        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(jsonString))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReplyPreview(
    senderName: String,
    text: String,
    accentColor: Color,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(
                    color = accentColor,
                    shape = ContinuousRoundedRectangle(2.dp)
                )
        )
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        ) {
            Text(
                text = senderName,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MediaAlbumGrid(messages: List<Message>) {
    val columns = if (messages.size >= 2) 2 else 1
    val rows = (messages.size + columns - 1) / columns

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until columns) {
                    val idx = row * columns + col
                    if (idx < messages.size) {
                        val msg = messages[idx]
                        val fileUrl = msg.fileUrl
                        if (!fileUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(java.io.File(fileUrl))
                                    .build(),
                                contentDescription = "Album photo",
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 80.dp, max = 200.dp)
                                    .padding(1.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .padding(1.dp)
                                    .background(Color.Gray.copy(alpha = 0.2f))
                            )
                        }
                    } else {
                        // Empty spacer for uneven grid
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
