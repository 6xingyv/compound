package com.mocharealm.compound.ui.screen.chat

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.composable.BackNavigationIcon
import com.mocharealm.compound.ui.shape.BubbleContinuousShape
import com.mocharealm.compound.ui.shape.BubbleSide
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
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
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .scrollEndHaptic(),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = innerPadding.calculateTopPadding(),
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
                items(state.messages.reversed(), key = { it.id }) { message ->
                    MessageBubble(message = message, viewModel = viewModel)
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
private fun MessageBubble(message: Message, viewModel: ChatViewModel) {
    val rowPadding = if (message.isOutgoing) {
        Modifier.padding(start = 64.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
    } else {
        Modifier.padding(start = 12.dp, end = 64.dp, top = 8.dp, bottom = 8.dp)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(rowPadding),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // 接收到的消息显示头像
        if (!message.isOutgoing) {
            Avatar(
                initials = message.senderName.take(2).uppercase(),
                modifier = Modifier.size(36.dp), // 稍微调大一点匹配气泡高度
                photoPath = message.avatarUrl
            )
            Spacer(Modifier.width(8.dp))
        }
        val shape = remember {
            BubbleContinuousShape(
                if (message.isOutgoing) BubbleSide.Right else BubbleSide.Left,
                CornerSize(20.dp)
            )
        }
        Column(
            Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!message.isOutgoing) {
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
            CompositionLocalProvider(
                LocalContentColor provides
                        if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
                        else MiuixTheme.colorScheme.onSurfaceContainer,
            ) {
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
                        MessageContent(message)
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
private fun MessageContent(message: Message) {
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
                            .heightIn(max = 300.dp) // 限制最大高度
                            .wrapContentWidth(),    // 宽度根据比例自适应
                        contentScale = ContentScale.Fit // 确保不被裁剪
                    )

                    val caption = message.content.removePrefix("Photo: ").takeIf { it != "Photo" }
                    if (!caption.isNullOrBlank()) {
                        Text(
                            text = caption,
                            style = MiuixTheme.textStyles.body1,
                            modifier = Modifier
                                .padding(top = 10.dp, bottom = 18.dp)
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            } else {
                Text(
                    text = message.content,
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 18.dp)
                        .padding(horizontal = 12.dp)
                )
            }
        }

        MessageType.STICKER -> {
            if (!message.fileUrl.isNullOrEmpty()) {
                if (message.stickerFormat == StickerFormat.WEBM) {
                    LoopingVideoSticker(
                        filePath = message.fileUrl,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(ContinuousRoundedRectangle(8.dp))
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(java.io.File(message.fileUrl))
                            .build(),
                        contentDescription = "Sticker",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(ContinuousRoundedRectangle(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Text(
                    text = message.content,
                    style = MiuixTheme.textStyles.body1,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 18.dp)
                        .padding(horizontal = 12.dp)
                )
            }
        }

        else -> Text(
            text = message.content,
            style = MiuixTheme.textStyles.body1,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 18.dp)
                .padding(horizontal = 12.dp)
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

