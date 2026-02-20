package com.mocharealm.compound.ui.screen.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.domain.model.SystemActionType
import com.mocharealm.compound.ui.EmptyIndication
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.composable.VideoPlayer
import com.mocharealm.compound.ui.screen.chat.composable.ShareSourceCard
import com.mocharealm.compound.ui.shape.BubbleContinuousShape
import com.mocharealm.compound.ui.shape.BubbleSide
import com.mocharealm.compound.ui.util.MarkdownTransformation
import com.mocharealm.compound.ui.util.SpoilerShader
import com.mocharealm.compound.ui.util.buildAnnotatedString
import com.mocharealm.compound.ui.util.formatMessageTimestamp
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.LayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.gaze.ui.composable.OverlayPositionProvider
import com.mocharealm.gaze.ui.composable.PopupMenu
import com.mocharealm.gaze.ui.composable.TextField
import com.mocharealm.gaze.ui.layout.imeNestedScroll
import com.mocharealm.gaze.ui.layout.imePadding
import com.mocharealm.gaze.ui.modifier.surface
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.hypot

private val LocalVideoDownloadProgress = staticCompositionLocalOf<Map<Long, Int>> { emptyMap() }
private val LocalOnDownloadVideo = staticCompositionLocalOf<(Long) -> Unit> { {} }

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

    val layerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val containerWidth = LocalWindowInfo.current.containerDpSize.width
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val focusRequester = remember { FocusRequester() }
    val menuOpened = remember { mutableStateOf(false) }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.loading && state.hasMore) {
            viewModel.loadOlderMessages()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LiquidSurface(
                        layerBackdrop, Modifier.size(48.dp), Modifier.clickable {
                            navigator.pop()
                        }, effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        }, shadow = {
                            Shadow(
                                radius = 24f.dp,
                                offset = DpOffset(0.dp, 0.dp),
                                color = Color.Black.copy(alpha = 0.1f),
                                alpha = 1f,
                                blendMode = DrawScope.DefaultBlendMode
                            )
                        },
                    ) {
                        Icon(
                            SFIcons.Chevron_Left,
                            null,
                            Modifier
                                .align(Alignment.Center)
                                .graphicsLayer {
                                    if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                                }
                        )
                    }
                    LiquidSurface(
                        layerBackdrop, Modifier.size(48.dp), effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        }, shadow = {
                            Shadow(
                                radius = 24f.dp,
                                offset = DpOffset(0.dp, 0.dp),
                                color = Color.Black.copy(alpha = 0.1f),
                                alpha = 1f,
                                blendMode = DrawScope.DefaultBlendMode
                            )
                        },
                    ) {
                        Icon(
                            SFIcons.Video_Fill,
                            null,
                            Modifier
                                .align(Alignment.Center)
                        )
                    }
                }
                state.chatInfo?.let { chatInfo ->
                    Column(
                        Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy((-4).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(
                            chatInfo.title.take(2),
                            modifier = Modifier
                                .size(48.dp)
                                .zIndex(20f),
                            photoPath = chatInfo.photoUrl
                        )
                        LiquidSurface(
                            layerBackdrop,
                            Modifier
                                .widthIn(max = (containerWidth - 160.dp).coerceAtLeast(0.dp))
                        ) {
                            Row(
                                Modifier.padding(8.dp, 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    chatInfo.title,
                                    style = MiuixTheme.textStyles.footnote1,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
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
                                    0f to surfaceColor.copy(0f), 1f to surfaceColor.copy(1f)
                                )
                            )
                        }
                    }
                    .navigationBarsPadding()
                    .captionBarPadding()
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom) {
                Spacer(Modifier.width(16.dp))
                Box(Modifier.size(48.dp)) {
                    androidx.compose.animation.AnimatedVisibility(
                        !menuOpened.value, Modifier.dropShadow(CircleShape) {
                            radius = 24f.dp.toPx()
                            offset = Offset(0.dp.toPx(), 0.dp.toPx())
                            color = Color.Black.copy(alpha = 0.1f)
                        }, enter = fadeIn(), exit = fadeOut()
                    ) {
                        LiquidSurface(
                            layerBackdrop, Modifier.fillMaxSize(), Modifier.clickable {
                                menuOpened.value = true
                            }, effects = {
                                vibrancy()
                                blur(1.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx())
                            }, shadow = {
                                Shadow(
                                    radius = 0.dp,
                                    offset = DpOffset(0.dp, 0.dp),
                                    color = Color.Transparent,
                                    alpha = 1f,
                                    blendMode = DrawScope.DefaultBlendMode
                                )
                            }, surfaceColor = surfaceContainerColor.copy(alpha = 0.6f)
                        ) {
                            Icon(SFIcons.Plus, null, Modifier.align(Alignment.Center))
                        }
                    }
                    PopupMenu(
                        menuOpened,
                        layerBackdrop,
                        popupPositionProvider = OverlayPositionProvider,
                        alignment = PopupPositionProvider.Align.BottomStart,
                        surfaceColor = surfaceContainerColor.copy(0.4f),
                        onDismissRequest = {
                            menuOpened.value = false
                        },
                        effects = {
                            blur(8.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        }
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val list = listOf(
                                tdString("AttachSticker") to SFIcons.Face_Smiling,
                                tdString("ChatGallery") to SFIcons.Photo_On_Rectangle_Angled,
                                tdString("ChatDocument") to SFIcons.Document,
                                tdString("ChatLocation") to SFIcons.Mappin
                            )
                            list.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        item.second,
                                        null,
                                        Modifier
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    0f to Color.Gray,
                                                    1f to Color.DarkGray
                                                )
                                            )
                                            .padding(8.dp)
                                            .size(20.dp),
                                        tint = Color.White
                                    )
                                    Text(item.first)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))
                CompositionLocalProvider(LocalIndication provides EmptyIndication) {
                    LiquidSurface(
                        layerBackdrop,
                        Modifier.weight(1f),
                        shape = { ContinuousRoundedRectangle(24.dp) },
                        effects = {
                            vibrancy()
                            blur(2.dp.toPx())
                            lens(15.dp.toPx(), 30.dp.toPx(), chromaticAberration = false)
                        },
                        surfaceColor = surfaceContainerColor.copy(alpha = 0.2f),
                        shadow = {
                            Shadow(
                                radius = 24f.dp,
                                offset = DpOffset(0.dp, 0.dp),
                                color = Color.Black.copy(alpha = 0.1f),
                                alpha = 1f,
                                blendMode = DrawScope.DefaultBlendMode
                            )
                        }
                    ) {
                        Row(
                            Modifier
                                .padding(start = 12.dp)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var inAudioMode by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = inAudioMode,
                                    enter = fadeIn() + slideInHorizontally { it },
                                    exit = fadeOut() + slideOutHorizontally { it }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            SFIcons.Microphone,
                                            null,
                                            modifier = Modifier.clickable {})
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = tdString("AccDescrVoiceMessage"),
                                            Modifier.weight(1f),
                                            style = MiuixTheme.textStyles.body1,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !inAudioMode,
                                    enter = fadeIn() + slideInHorizontally { -it },
                                    exit = fadeOut() + slideOutHorizontally { -it }) {
                                    TextField(
                                        modifier = Modifier
                                            .fillMaxWidth()
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
                                        textStyle = MiuixTheme.textStyles.body1,
                                        decorator = { innerTextField ->
                                            if (viewModel.inputState.text.isEmpty()) {
                                                Box {
                                                    innerTextField()
                                                    Text(
                                                        tdString("TypeMessage", "default"),
                                                        color = LocalContentColor.current.copy(0.4f),
                                                        style = MiuixTheme.textStyles.body1,
                                                    )
                                                }
                                            } else innerTextField()
                                        }
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = (viewModel.inputState.text.lines().size <= 1 || inAudioMode) && !state.loading,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .clickable {
                                            inAudioMode = !inAudioMode
                                        }) {
                                    AnimatedContent(
                                        targetState = inAudioMode, transitionSpec = {
                                            (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                                        }, label = "IconSwitch"
                                    ) { isAudio ->
                                        if (isAudio) {
                                            Icon(SFIcons.Xmark, contentDescription = "Close Audio")
                                        } else {
                                            Icon(
                                                SFIcons.Microphone,
                                                contentDescription = "Open Audio"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
                AnimatedVisibility(
                    !state.loading && viewModel.inputState.text.isNotBlank(),
                    Modifier,
                    enter = fadeIn() + slideInHorizontally { if (layoutDirection == LayoutDirection.Ltr) it else -it } + expandHorizontally(),
                    exit = fadeOut() + slideOutHorizontally { if (layoutDirection == LayoutDirection.Ltr) it else -it } + shrinkHorizontally()) {
                    LiquidSurface(
                        layerBackdrop,
                        Modifier
                            .padding(end = 16.dp)
                            .size(48.dp),
                        Modifier.clickable(onClick = viewModel::sendMessage),
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        },
                        tint = primaryColor,
                        shadow = {
                            Shadow(
                                radius = 16.dp,
                                offset = DpOffset(0.dp, 0.dp),
                                alpha = 1f,
                                blendMode = DrawScope.DefaultBlendMode
                            )
                        },
                    ) {
                        Icon(
                            SFIcons.Paperplane, null, Modifier.align(Alignment.Center), Color.White
                        )
                    }
                }
            }
        }) { innerPadding ->
        CompositionLocalProvider(
            LocalVideoDownloadProgress provides state.videoDownloadProgress,
            LocalOnDownloadVideo provides { messageId: Long -> viewModel.downloadVideo(messageId) }
        ) {
            val onReplyClick: (Long) -> Unit = { replyMessageId ->
                viewModel.scrollToMessage(replyMessageId)
            }

            val scrollTarget = state.scrollToMessageId
            LaunchedEffect(scrollTarget, state.chatItems) {
                if (scrollTarget != null) {
                    val targetIdx = state.chatItems.indexOfFirst {
                        it is MessageItem && it.messages.any { m -> m.id == scrollTarget }
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
                    .layerBackdrop(layerBackdrop)
                    .fillMaxSize()
                    .imeNestedScroll(focusRequester)
                    .scrollEndHaptic(),
                contentPadding = innerPadding,
                overscrollEffect = null,
            ) {
                if (state.loading && state.messages.isEmpty()) {
                    item {
                        Card(modifier = Modifier.padding(12.dp)) {
                            BasicComponent(title = tdString("Loading"))
                        }
                    }
                } else if (state.error != null && state.messages.isEmpty()) {
                    item {
                        Card(modifier = Modifier.padding(12.dp)) {
                            BasicComponent(
                                title = tdString("ErrorOccurred"),
                                summary = state.error,
                            )
                            TextButton(
                                text = tdString("Retry"),
                                onClick = { viewModel.loadMessages() },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                } else {
                    items(
                        count = state.chatItems.size,
                        key = { state.chatItems[it].key }
                    ) { index ->
                        when (val item = state.chatItems[index]) {
                            is TimestampItem -> {
                                TimestampLabel(timestamp = item.timestamp)
                            }

                            is MessageItem -> {
                                val primaryMessage = item.primaryMessage
                                if (primaryMessage.messageType == MessageType.SYSTEM) {
                                    SystemMessage(primaryMessage)
                                } else if (item.isAlbum) {
                                    MessageBubble(
                                        message = primaryMessage,
                                        groupPosition = item.groupPosition,
                                        albumMessages = item.messages,
                                        onReplyClick = onReplyClick,
                                    )
                                } else {
                                    MessageBubble(
                                        message = primaryMessage,
                                        groupPosition = item.groupPosition,
                                        onReplyClick = onReplyClick,
                                    )
                                }
                            }
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
                                    text = tdString("Loading"),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimestampLabel(timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = timestamp.formatMessageTimestamp(),
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            textAlign = TextAlign.Center,
            style = MiuixTheme.textStyles.footnote1
        )
    }
}

@Composable
private fun SystemMessage(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val parts = remember(message.content) { message.content.split("|") }
        val typeIdx = parts.getOrNull(0)?.toIntOrNull() ?: -1
        val type = SystemActionType.entries.getOrNull(typeIdx)

        val text = when (type) {
            SystemActionType.MEMBER_JOINED -> tdString(
                "NotificationGroupAddMember",
                "un1" to (parts.getOrNull(1) ?: ""),
                "un2" to (parts.getOrNull(2) ?: "")
            )

            SystemActionType.MEMBER_JOINED_BY_LINK -> tdString(
                "ActionInviteUser",
                "un1" to (parts.getOrNull(1) ?: "")
            )

            SystemActionType.MEMBER_LEFT -> tdString(
                "EventLogLeftGroup",
                "un1" to (parts.getOrNull(1) ?: "")
            )

            SystemActionType.CHAT_CHANGED_TITLE -> tdString(
                "ActionChatEditTitle",
                "un1" to (parts.getOrNull(1) ?: ""),
                "title" to (parts.getOrNull(2) ?: "")
            )

            SystemActionType.CHAT_CHANGED_PHOTO -> tdString(
                "ActionChatEditPhoto",
                "un1" to (parts.getOrNull(1) ?: "")
            )

            SystemActionType.CHAT_UPGRADED_TO -> tdString(
                "ActionChatUpgradeTo",
                "supergroupId" to (parts.getOrNull(1) ?: "")
            )

            SystemActionType.PIN_MESSAGE -> tdString(
                "ActionPinnedText",
                "un1" to (parts.getOrNull(1) ?: "")
            )

            null -> message.content
        }

        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            textAlign = TextAlign.Center,
            style = MiuixTheme.textStyles.footnote1
        )
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
            Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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

            CompositionLocalProvider(
                LocalContentColor provides if (message.isOutgoing) MiuixTheme.colorScheme.onPrimary
                else MiuixTheme.colorScheme.onSurfaceContainer,
            ) {
                if (isSticker) {
                    MessageContent(
                        message,
                        hasTail = isLast,
                        albumMessages = null,
                        onReplyClick = onReplyClick
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
private fun MessageContent(
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
            .widthIn(min = 44.dp)
    } else {
        Modifier.widthIn(min = 44.dp)
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
                    albumMessages,
                    modifier = Modifier.widthIn(max = 280.dp)
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
                    top = fileTop,
                    bottom = fileBottom,
                    start = 12.dp,
                    end = 12.dp
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
                text = richText, contentColor = contentColor, uriHandler = uriHandler,
                revealedEntityIndices = revealedSpoilers.value,
                onSpoilerClick = { index -> revealedSpoilers.value += index },
                // 去掉了原本错误赋予的 fillMaxWidth()
                modifier = Modifier.padding(
                    top = textTop,
                    bottom = textBottom,
                    start = 12.dp,
                    end = 12.dp
                )
            )
        }

        if (hasShare) {
            val shareTop = if (hasReply || hasMedia || isDocument || hasText) 8.dp else 10.dp
            ShareSourceCard(
                shareInfo = message.shareInfo,
                modifier = Modifier.padding(
                    top = shareTop,
                    bottom = bottomPadding,
                    start = 12.dp,
                    end = 12.dp
                )
            )
        }
    }
}

@Composable
private fun ReplyPreview(
    senderName: String,
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            // 此处删除了 .fillMaxWidth()
            .clip(ContinuousRoundedRectangle(8.dp))
            .background(MiuixTheme.colorScheme.onSurfaceContainer.copy(0.1f))
            .clickable(onClick = onClick)
            .drawWithCache {
                onDrawBehind {
                    drawRect(accentColor, size = Size(4.dp.toPx(), size.height))
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .padding(start = 4.dp)
                .padding(8.dp)
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
private fun PhotoBlock(message: Message) {
    if (!message.fileUrl.isNullOrEmpty()) {
        SpoilerImage(hasSpoiler = message.hasSpoiler, modifier = Modifier.wrapContentWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(java.io.File(message.fileUrl)).build(),
                contentDescription = "Photo",
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun VideoBlock(message: Message) {
    val maxWidth = 280.dp
    val aspectRatio = if (message.mediaWidth > 0 && message.mediaHeight > 0)
        message.mediaWidth.toFloat() / message.mediaHeight.toFloat() else 16f / 9f
    val videoModifier = Modifier
        .width(maxWidth)
        .height(maxWidth / aspectRatio)

    SpoilerImage(hasSpoiler = message.hasSpoiler, modifier = Modifier.wrapContentSize()) {
        if (!message.fileUrl.isNullOrEmpty()) {
            MessageVideoPlayer(filePath = message.fileUrl, modifier = videoModifier)
        } else {
            VideoThumbnailOverlay(message = message, modifier = videoModifier)
        }
    }
}

@Composable
private fun StickerBlock(message: Message, modifier: Modifier = Modifier) {
    if (!message.fileUrl.isNullOrEmpty()) {
        when (message.stickerFormat) {
            StickerFormat.WEBM -> VideoPlayer(filePath = message.fileUrl, modifier = modifier)
            StickerFormat.TGS -> LottieSticker(filePath = message.fileUrl, modifier = modifier)
            else -> AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(java.io.File(message.fileUrl)).build(),
                contentDescription = "Sticker",
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        }
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
                                        ), center = origin, radius = radius
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
            .pointerInput(text, layoutResult, revealedEntityIndices) {
                detectTapGestures { pos ->
                    val layout = layoutResult.value ?: return@detectTapGestures
                    if (pos.y < 0 || pos.y > layout.size.height) return@detectTapGestures
                    val offset = layout.getOffsetForPosition(pos)

                    text.getStringAnnotations("URL", offset, offset).firstOrNull()
                        ?.let { uriHandler.openUri(it.item) }

                    text.getStringAnnotations("SPOILER", offset, offset).firstOrNull()?.let {
                        val index = it.item.toIntOrNull() ?: return@let
                        if (index in revealedEntityIndices || revealingSpoilers.containsKey(index)) return@let

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
            modifier = modifier
        )
    }
}

@Composable
fun VideoThumbnailOverlay(message: Message, modifier: Modifier = Modifier) {
    val downloadPercent = LocalVideoDownloadProgress.current[message.id]
    val onDownloadVideo = LocalOnDownloadVideo.current
    val layerBackdrop = rememberLayerBackdrop { drawRect(Color.Black); drawContent() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MiuixTheme.colorScheme.surfaceContainerHighest)
            .clickable { if (downloadPercent == null) onDownloadVideo(message.id) },
        contentAlignment = Alignment.Center
    ) {
        if (!message.thumbnailUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(java.io.File(message.thumbnailUrl)).build(),
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(layerBackdrop),
                contentScale = ContentScale.Crop
            )
        }

        LiquidSurface(
            layerBackdrop,
            if (downloadPercent != null) Modifier.padding(
                horizontal = 12.dp,
                vertical = 6.dp
            ) else Modifier.size(48.dp),
            Modifier.clickable { onDownloadVideo(message.id) },
            effects = {
                vibrancy()
                if (downloadPercent != null) blur(1.dp.toPx())
                lens(if (downloadPercent != null) 16.dp.toPx() else 8.dp.toPx(), 32.dp.toPx())
            },
            surfaceColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.6f)
        ) {
            if (downloadPercent != null) {
                Text(
                    text = "$downloadPercent%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Icon(SFIcons.Play_Fill, null, Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun MessageVideoPlayer(filePath: String, modifier: Modifier = Modifier) {
    var isControlsVisible by remember { mutableStateOf(false) }
    val layerBackdrop = rememberLayerBackdrop { drawRect(Color.Black); drawContent() }
    VideoPlayer(
        filePath = filePath,
        modifier = modifier,
        playerSurfaceModifier = Modifier.layerBackdrop(layerBackdrop),
        loop = false,
        mute = false,
        playWhenReady = false,
        gestureHandler = { detectTapGestures { isControlsVisible = !isControlsVisible } },
        playerControls = { player ->
            ControlLayer(
                layerBackdrop = layerBackdrop,
                player = player,
                isVisible = isControlsVisible,
                onVisibilityChange = { isControlsVisible = it })
        }
    )
}

@Composable
private fun BoxScope.ControlLayer(
    layerBackdrop: LayerBackdrop,
    player: ExoPlayer,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit
) {
    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(2000)
            onVisibilityChange(false)
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center)
    ) {
        LiquidSurface(
            layerBackdrop, Modifier.size(48.dp),
            Modifier.clickable { if (player.isPlaying) player.pause() else player.play() },
            effects = { vibrancy(); lens(8.dp.toPx(), 16.dp.toPx()) },
            shadow = {
                Shadow(
                    radius = 0.dp,
                    offset = DpOffset(0.dp, 0.dp),
                    color = Color.Transparent,
                    alpha = 1f,
                    blendMode = DrawScope.DefaultBlendMode
                )
            },
            surfaceColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.6f)
        ) {
            AnimatedContent(isPlaying, Modifier.align(Alignment.Center)) { playing ->
                if (playing) Icon(SFIcons.Pause_Fill, null) else Icon(SFIcons.Play_Fill, null)
            }
        }
    }
}

@Composable
private fun MediaAlbumGrid(messages: List<Message>, modifier: Modifier = Modifier) {
    val columns = if (messages.size >= 2) 2 else 1
    val rows = (messages.size + columns - 1) / columns

    Column(modifier = modifier) {
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
                                if (msg.messageType == MessageType.VIDEO) {
                                    VideoPlayer(
                                        filePath = fileUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        loop = false,
                                        mute = true
                                    )
                                } else {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(java.io.File(fileUrl)).build(),
                                        contentDescription = "Album photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
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
                            center = origin, radius = radius, blendMode = BlendMode.DstOut
                        )
                    }
                    canvas.restore()
                }) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(24.dp)
                    .drawWithCache {
                        onDrawWithContent { drawContent(); drawRect(Color.Black.copy(0.2f)) }
                    }) {
                content()
            }
            Canvas(modifier = Modifier.matchParentSize()) {
                shader.setFloatUniform("particleColor", 1f, 1f, 1f, 1f)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("resolution", size.width, size.height)
                drawRect(brush = brush, blendMode = BlendMode.Plus)
            }
        }
    }
}