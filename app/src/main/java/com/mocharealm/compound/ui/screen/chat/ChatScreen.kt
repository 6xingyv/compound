package com.mocharealm.compound.ui.screen.chat

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
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
import com.mocharealm.compound.ui.composable.TextField
import com.mocharealm.compound.ui.layout.imeNestedScroll
import com.mocharealm.compound.ui.layout.imePadding
import com.mocharealm.compound.ui.modifier.surface
import com.mocharealm.compound.ui.shape.BubbleContinuousShape
import com.mocharealm.compound.ui.shape.BubbleSide
import com.mocharealm.compound.ui.util.MarkdownTransformation
import com.mocharealm.compound.ui.util.SpoilerShader
import com.mocharealm.compound.ui.util.buildAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousCapsule
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.drawBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.hypot

private enum class GroupPosition { FIRST, MIDDLE, LAST, SINGLE }

private data class DisplayItem(
    val messages: List<Message>,
    val isAlbum: Boolean
)

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val navigator = LocalNavigator.current
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
        }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val surfaceContainerColor = MiuixTheme.colorScheme.surfaceContainer
    val primaryColor = MiuixTheme.colorScheme.primary

    val glassyState = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val scope = rememberCoroutineScope()

    val backScale = remember { Animatable(0f) }
    val moreScale = remember { Animatable(0f) }
    val inputScale = remember { Animatable(0f) }
    val sendScale = remember { Animatable(0f) }

    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.loading && state.hasMore) {
            viewModel.loadOlderMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        popupHost = {},
        topBar = {
            Box(
                Modifier
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to surfaceColor.copy(0.4f),
                                    1f to surfaceColor.copy(0f),
                                    startY = statusBarHeightPx.toFloat()
                                )
                            )
                        }
                    }
                    .statusBarsPadding()) {
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                ) {
                    BackNavigationIcon(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .drawBackdrop(
                                glassyState, shape = { CircleShape },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx())
                                    lens(16.dp.toPx(), 32.dp.toPx())
                                },
                                onDrawSurface = { drawRect(surfaceContainerColor.copy(alpha = 0.6f)) },
                                layerBlock = {
                                    val progress = backScale.value
                                    val maxScale = (size.width + 16f.dp.toPx()) / size.width
                                    val scale = lerp(1f, maxScale, progress)
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                            .pointerInput(scope) {
                                val animationSpec = spring(0.5f, 300f, 0.001f)
                                awaitEachGesture {
                                    // press
                                    awaitFirstDown()
                                    scope.launch {
                                        backScale.animateTo(1f, animationSpec)
                                    }

                                    // release
                                    waitForUpOrCancellation()
                                    scope.launch {
                                        backScale.animateTo(0f, animationSpec)
                                    }
                                }
                            },
                        onClick = { navigator.pop() },
                    )
                }
                state.chatInfo?.let { chatInfo ->
                    Column(
                        Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy(-4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(
                            chatInfo.title.take(2),
                            modifier = Modifier
                                .size(48.dp)
                                .zIndex(20f),
                            photoPath = chatInfo.photoUrl
                        )
                        Text(
                            chatInfo.title,
                            style = MiuixTheme.textStyles.footnote1,
                            modifier = Modifier
                                .drawBackdrop(
                                    glassyState, shape = { ContinuousCapsule },
                                    effects = {
                                        vibrancy()
                                        blur(4.dp.toPx())
                                        lens(8.dp.toPx(), 16.dp.toPx())
                                    },
                                    onDrawSurface = { drawRect(surfaceContainerColor.copy(alpha = 0.1f)) },
                                )
                                .padding(8.dp, 4.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Row(
                Modifier
                    .imePadding()
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to surfaceColor.copy(0f),
                                    1f to surfaceColor.copy(1f)
                                )
                            )
                        }
                    }
                    .navigationBarsPadding()
                    .captionBarPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier
                        .drawBackdrop(
                            glassyState, shape = { CircleShape },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            },
                            onDrawSurface = { drawRect(surfaceContainerColor.copy(alpha = 0.6f)) },
                            shadow = {
                                Shadow(
                                    radius = 24f.dp,
                                    offset = DpOffset(0.dp, 0.dp),
                                    color = Color.Black.copy(alpha = 0.1f),
                                    alpha = 1f,
                                    blendMode = DrawScope.DefaultBlendMode
                                )
                            },
                            layerBlock = {
                                val progress = moreScale.value
                                val maxScale = (size.width + 16f.dp.toPx()) / size.width
                                val scale = lerp(1f, maxScale, progress)
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                        .pointerInput(scope) {
                            val animationSpec = spring(0.5f, 300f, 0.001f)
                            awaitEachGesture {
                                // press
                                awaitFirstDown()
                                scope.launch {
                                    moreScale.animateTo(1f, animationSpec)
                                }

                                // release
                                waitForUpOrCancellation()
                                scope.launch {
                                    moreScale.animateTo(0f, animationSpec)
                                }
                            }
                        }
                        .size(45.dp)
                )
                Spacer(Modifier.width(16.dp))
                Row(
                    Modifier
                        .weight(1f)
                        .drawBackdrop(
                            glassyState,
                            shape = { ContinuousRoundedRectangle(24.dp) },
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx())
                                lens(10.dp.toPx(), 20.dp.toPx())
                            },
                            onDrawSurface = { drawRect(surfaceContainerColor.copy(alpha = 0.1f)) },
                            shadow = {
                                Shadow(
                                    radius = 24f.dp,
                                    offset = DpOffset(0.dp, 0.dp),
                                    color = Color.Black.copy(alpha = 0.1f),
                                    alpha = 1f,
                                    blendMode = DrawScope.DefaultBlendMode
                                )
                            },
                            layerBlock = {
                                val progress = inputScale.value
                                val maxScale = (size.width + 16f.dp.toPx()) / size.width
                                val scale = lerp(1f, maxScale, progress)
                                scaleX = scale
                                scaleY = scale
                            }
                        )
                        .pointerInput(scope) {
                            val animationSpec = spring(0.5f, 300f, 0.001f)
                            awaitEachGesture {
                                // press
                                awaitFirstDown()
                                scope.launch {
                                    inputScale.animateTo(1f, animationSpec)
                                }

                                // release
                                waitForUpOrCancellation()
                                scope.launch {
                                    inputScale.animateTo(0f, animationSpec)
                                }
                            }
                        }
                        .padding(12.dp)
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        state = viewModel.inputState,
                        outputTransformation = MarkdownTransformation,
                        lineLimits = TextFieldLineLimits.MultiLine(),
                        padding = 0.dp,
                        clipRadius = 0.dp,
                        activeBackgroundColor = Color.Transparent,
                        inactiveBackgroundColor = Color.Transparent,
                        activeBorderSize = 0.dp,
                        inactiveBorderSize = 0.dp,
                        textStyle = MiuixTheme.textStyles.body1
                    )
                }
                Spacer(Modifier.width(16.dp))
                AnimatedVisibility(
                    !state.loading && viewModel.inputState.text.isNotBlank(),
                    Modifier.dropShadow(ContinuousCapsule) {
                        color = Color.Black.copy(alpha = 0.1f)
                        radius = 16f.dp.toPx()
                    },
                    enter = fadeIn()
                            + slideInHorizontally { if (layoutDirection == LayoutDirection.Ltr) it else -it }
                            + expandHorizontally(),
                    exit = fadeOut()
                            + slideOutHorizontally { if (layoutDirection == LayoutDirection.Ltr) it else -it }
                            + shrinkHorizontally()
                ) {
                    Box(
                        Modifier
                            .padding(end = 16.dp)
                            .drawBackdrop(
                                glassyState, shape = { ContinuousCapsule },
                                effects = {
                                    vibrancy()
                                    blur(4.dp.toPx())
                                    lens(16.dp.toPx(), 32.dp.toPx())
                                },
                                onDrawSurface = {
                                    drawRect(primaryColor, blendMode = BlendMode.Hue)
                                    drawRect(primaryColor.copy(alpha = 0.75f))
                                },
                                shadow = {
                                    Shadow(
                                        radius = 0.dp,
                                        offset = DpOffset(0.dp, 0.dp),
                                        color = Color.Transparent,
                                        alpha = 1f,
                                        blendMode = DrawScope.DefaultBlendMode
                                    )
                                },
                                layerBlock = {
                                    val progress = sendScale.value
                                    val maxScale = (size.width + 16f.dp.toPx()) / size.width
                                    val scale = lerp(1f, maxScale, progress)
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                            .clickable(onClick = viewModel::sendMessage)
                            .pointerInput(scope) {
                                val animationSpec = spring(0.5f, 300f, 0.001f)
                                awaitEachGesture {
                                    // press
                                    awaitFirstDown()
                                    scope.launch {
                                        sendScale.animateTo(1f, animationSpec)
                                    }

                                    // release
                                    waitForUpOrCancellation()
                                    scope.launch {
                                        sendScale.animateTo(0f, animationSpec)
                                    }
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Text(
                            "Send",
                            style = MiuixTheme.textStyles.body1,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val displayMessages = state.messages.reversed()

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

        val onReplyClick: (Long) -> Unit = { replyMessageId ->
            viewModel.scrollToMessage(replyMessageId)
        }

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
                .layerBackdrop(glassyState)
                .fillMaxSize()
                .imeNestedScroll(focusRequester)
                .scrollEndHaptic(),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            if (state.loading && state.messages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
                        BasicComponent(title = "Loading messages...")
                    }
                }
            } else if (state.error != null && state.messages.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
                        BasicComponent(
                            title = "Error",
                            summary = state.error,
                        )
                        TextButton(
                            text = "Retry",
                            onClick = { viewModel.loadMessages() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else if (state.messages.isEmpty() && state.initialLoaded) {
                item {
                    Card(
                        modifier = Modifier
                            .padding(12.dp)
                    ) {
                        BasicComponent(title = "No messages in this chat")
                    }
                }
            } else {
                items(
                    displayItems.size,
                    key = { displayItems[it].messages.first().id }
                ) { index ->
                    val item = displayItems[index]
                    val primaryMessage = item.messages.first()

                    // Group position based on the primary message's senderId
                    val prevSender =
                        displayItems.getOrNull(index - 1)?.messages?.first()?.senderId
                    val nextSender =
                        displayItems.getOrNull(index + 1)?.messages?.first()?.senderId
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
                                color =
                                    if (message.isOutgoing) MiuixTheme.colorScheme.primary
                                    else MiuixTheme.colorScheme.surfaceContainer,
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

    // State for revealed spoilers
    val revealedSpoilers = rememberSaveable(message.id) { mutableStateOf(setOf<Int>()) }

    val richText = remember(message.content, message.entities, revealedSpoilers.value) {
        buildAnnotatedString(
            text = message.content,
            entities = message.entities,
            linkColor = linkColor,
            revealedEntityIndices = revealedSpoilers.value
        )
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
            val captionRich = remember(caption, message.entities, revealedSpoilers.value) {
                buildAnnotatedString(caption, message.entities, linkColor, revealedSpoilers.value)
            }
            RichTextContent(
                text = captionRich,
                contentColor = contentColor,
                modifier = textPadding,
                uriHandler = uriHandler,
                revealedEntityIndices = revealedSpoilers.value,
                onSpoilerClick = { index -> revealedSpoilers.value += index }
            )
        }
        return
    }

    when (message.messageType) {
        MessageType.PHOTO -> {
            if (!message.fileUrl.isNullOrEmpty()) {
                Column(modifier = Modifier.width(IntrinsicSize.Min)) {
                    SpoilerImage(
                        hasSpoiler = message.hasSpoiler,
                        modifier = Modifier.wrapContentWidth()
                    ) {
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
                    }

                    val caption = message.content.removePrefix("Photo: ").takeIf { it != "Photo" }
                    if (!caption.isNullOrBlank()) {
                        val captionRich =
                            remember(caption, message.entities, revealedSpoilers.value) {
                                buildAnnotatedString(
                                    caption,
                                    message.entities,
                                    linkColor,
                                    revealedSpoilers.value
                                )
                            }
                        RichTextContent(
                            text = captionRich,
                            contentColor = contentColor,
                            modifier = textPadding.fillMaxWidth(),
                            uriHandler = uriHandler,
                            revealedEntityIndices = revealedSpoilers.value,
                            onSpoilerClick = { index -> revealedSpoilers.value += index }
                        )
                    }
                }
            } else {
                RichTextContent(
                    text = richText,
                    contentColor = contentColor,
                    modifier = textPadding,
                    uriHandler = uriHandler,
                    revealedEntityIndices = revealedSpoilers.value,
                    onSpoilerClick = { index -> revealedSpoilers.value += index }
                )
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
                RichTextContent(
                    text = richText,
                    contentColor = contentColor,
                    modifier = textPadding,
                    uriHandler = uriHandler,
                    revealedEntityIndices = revealedSpoilers.value,
                    onSpoilerClick = { index -> revealedSpoilers.value += index }
                )
            }
        }

        else -> RichTextContent(
            text = richText,
            contentColor = contentColor,
            modifier = textPadding,
            uriHandler = uriHandler,
            revealedEntityIndices = revealedSpoilers.value,
            onSpoilerClick = { index -> revealedSpoilers.value += index }
        )
    }
}

@Composable
private fun RichTextContent(
    text: androidx.compose.ui.text.AnnotatedString,
    contentColor: Color,
    modifier: Modifier,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    revealedEntityIndices: Set<Int>,
    onSpoilerClick: (Int) -> Unit
) {
    if (text.spanStyles.isEmpty() && text.getStringAnnotations(0, text.length).isEmpty()) {
        Text(text = text.text, color = contentColor, modifier = modifier)
        return
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        var accumulatedNanos = 0L
        val thresholdNanos = 1_000_000_000_000L

        while (true) {
            withFrameNanos { frameTime ->
                val deltaNanos = frameTime - lastFrameTime
                lastFrameTime = frameTime
                accumulatedNanos += deltaNanos
                accumulatedNanos %= thresholdNanos
                time = (accumulatedNanos / 1_000_000_000f) * 0.65f
            }
        }
    }

    val shader = remember { SpoilerShader.getShader() }
    val brush = remember(shader) { ShaderBrush(shader) }

    val revealingSpoilers =
        remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    val revealingOrigins = remember { mutableStateMapOf<Int, Offset>() }
    val coroutineScope = rememberCoroutineScope()

    val spoilerPaths = remember(text, layoutResult.value) {
        val layout = layoutResult.value ?: return@remember emptyMap<Int, Path>()
        val paths = mutableMapOf<Int, Path>()
        text.getStringAnnotations("SPOILER", 0, text.length).forEach { range ->
            range.item.toIntOrNull()?.let { index ->
                paths[index] = layout.multiParagraph.getPathForRange(range.start, range.end)
            }
        }
        paths
    }

    Text(
        text = text,
        color = contentColor,
        onTextLayout = { layoutResult.value = it },
        modifier = modifier
            .drawWithContent {
                val obscuredPath = Path()
                var hasObscured = false
                spoilerPaths.forEach { (index, path) ->
                    if (index !in revealedEntityIndices || index in revealingSpoilers) {
                        obscuredPath.addPath(path)
                        hasObscured = true
                    }
                }

                if (!hasObscured) {
                    drawContent()
                    return@drawWithContent
                }

                clipPath(obscuredPath, clipOp = ClipOp.Difference) {
                    this@drawWithContent.drawContent()
                }

                val hasRipples = revealingSpoilers.isNotEmpty()

                if (hasRipples) {
                    clipPath(obscuredPath) {
                        val canvas = drawContext.canvas
                        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())

                        revealingSpoilers.forEach { (index, anim) ->
                            revealingOrigins[index]?.let { origin ->
                                val radius = anim.value
                                if (radius > 0f) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            0.0f to Color.Black,
                                            0.5f to Color.Black,
                                            1.0f to Color.Transparent,
                                            center = origin,
                                            radius = radius
                                        ),
                                        center = origin,
                                        radius = radius
                                    )
                                }
                            }
                        }

                        canvas.saveLayer(
                            Rect(0f, 0f, size.width, size.height),
                            Paint().apply { blendMode = BlendMode.SrcIn })
                        this@drawWithContent.drawContent()
                        canvas.restore()

                        canvas.restore()
                    }
                }

                clipPath(obscuredPath) {
                    val canvas = drawContext.canvas
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())

                    shader.setFloatUniform(
                        "particleColor",
                        contentColor.red,
                        contentColor.green,
                        contentColor.blue,
                        contentColor.alpha
                    )
                    shader.setFloatUniform("time", time)
                    shader.setFloatUniform("resolution", size.width, size.height)
                    drawRect(brush = brush)

                    if (hasRipples) {
                        revealingSpoilers.forEach { (index, anim) ->
                            revealingOrigins[index]?.let { origin ->
                                val radius = anim.value
                                if (radius > 0f) {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            0.0f to Color.Black,
                                            0.5f to Color.Black,
                                            1.0f to Color.Transparent,
                                            center = origin,
                                            radius = radius
                                        ),
                                        center = origin,
                                        radius = radius,
                                        blendMode = BlendMode.DstOut
                                    )
                                }
                            }
                        }
                    }

                    canvas.restore()
                }
            }
            .pointerInput(text, layoutResult, revealedEntityIndices) { // 关键：将 revealedEntityIndices 加入 key
                detectTapGestures { pos ->
                    val layout = layoutResult.value ?: return@detectTapGestures
                    if (pos.y < 0 || pos.y > layout.size.height) return@detectTapGestures
                    val offset = layout.getOffsetForPosition(pos)

                    text.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                        uriHandler.openUri(it.item)
                    }

                    text.getStringAnnotations("SPOILER", offset, offset).firstOrNull()?.let {
                        val index = it.item.toIntOrNull() ?: return@let

                        if (index in revealedEntityIndices || revealingSpoilers.containsKey(index)) {
                            return@let
                        }

                        val maxRadius = hypot(
                            layout.size.width.toDouble(),
                            layout.size.height.toDouble()
                        ).toFloat()

                        val anim = Animatable(0f)
                        revealingSpoilers[index] = anim
                        revealingOrigins[index] = pos

                        coroutineScope.launch {
                            anim.animateTo(
                                targetValue = maxRadius,
                                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                            )
                            onSpoilerClick(index)
                            yield()
                            revealingSpoilers.remove(index)
                            revealingOrigins.remove(index)
                        }
                    }
                }
            }
    )
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

    PlayerSurface(exoPlayer, modifier, surfaceType = SURFACE_TYPE_TEXTURE_VIEW)
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
                            SpoilerImage(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 80.dp, max = 200.dp)
                                    .padding(1.dp),
                                hasSpoiler = msg.hasSpoiler
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(java.io.File(fileUrl))
                                        .build(),
                                    contentDescription = "Album photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
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
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpoilerImage(
    modifier: Modifier = Modifier,
    hasSpoiler: Boolean,
    content: @Composable () -> Unit
) {
    if (!hasSpoiler) {
        content()
        return
    }

    var isRevealed by rememberSaveable { mutableStateOf(false) }

    if (isRevealed) {
        content()
        return
    }

    val shader = remember { SpoilerShader.getShader() }
    val brush = remember(shader) { ShaderBrush(shader) }

    val revealAnim = remember { Animatable(0f) }
    val revealOrigin = remember { mutableStateOf(Offset.Zero) }
    val coroutineScope = rememberCoroutineScope()

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrameTime = withFrameNanos { it }
        var accumulatedNanos = 0L
        val thresholdNanos = 1_000_000_000_000L

        while (true) {
            withFrameNanos { frameTime ->
                val deltaNanos = frameTime - lastFrameTime
                lastFrameTime = frameTime
                accumulatedNanos += deltaNanos
                accumulatedNanos %= thresholdNanos
                time = (accumulatedNanos / 1_000_000_000f) * 0.65f
            }
        }
    }

    Box(modifier = modifier) {
        content()
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        if (isRevealed) return@detectTapGestures

                        revealOrigin.value = pos
                        val maxRadius =
                            hypot(size.width.toDouble(), size.height.toDouble()).toFloat()

                        coroutineScope.launch {
                            revealAnim.animateTo(
                                targetValue = maxRadius,
                                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                            )
                            isRevealed = true
                        }
                    }
                }
                .drawWithContent {
                    val radius = revealAnim.value
                    val origin = revealOrigin.value

                    val canvas = drawContext.canvas
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())

                    drawContent()

                    if (radius > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                0.0f to Color.Black,
                                0.5f to Color.Black,
                                1.0f to Color.Transparent,
                                center = origin,
                                radius = radius
                            ),
                            center = origin,
                            radius = radius,
                            blendMode = BlendMode.DstOut
                        )
                    }

                    canvas.restore()
                }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(24.dp)
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(0.2f))
                        }
                    }
            ) {
                content()
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                shader.setFloatUniform("particleColor", 1f, 1f, 1f, 1f)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                drawRect(brush = brush)
            }
        }
    }
}