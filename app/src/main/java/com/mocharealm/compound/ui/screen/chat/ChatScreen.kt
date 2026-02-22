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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.domain.model.SystemActionType
import com.mocharealm.compound.ui.EmptyIndication
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.composable.base.VideoPlayer
import com.mocharealm.compound.ui.composable.chat.MessageBubble
import com.mocharealm.compound.ui.composable.chat.SystemMessage
import com.mocharealm.compound.ui.composable.chat.TimestampLabel
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

 val LocalVideoDownloadProgress = staticCompositionLocalOf<Map<Long, Int>> { emptyMap() }
 val LocalOnDownloadVideo = staticCompositionLocalOf<(Long) -> Unit> { {} }

@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                        layerBackdrop, Modifier.size(48.dp),
                        Modifier.clickable {
                            navigator.pop()
                        },
                        effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        },
                        shadow = {
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
                                })
                    }
                    LiquidSurface(
                        layerBackdrop, Modifier.size(48.dp),
                        effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        },
                        shadow = {
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
                            SFIcons.Video_Fill, null, Modifier.align(Alignment.Center)
                        )
                    }
                }
                state.chatInfo?.let { chatInfo ->
                    Column(
                        Modifier.align(Alignment.Center),
                        verticalArrangement = Arrangement.spacedBy((-6).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Avatar(
                            initials = chatInfo.title.take(2),
                            modifier = Modifier
                                .size(48.dp)
                                .zIndex(20f),
                            photoPath = chatInfo.photoUrl
                        )
                        LiquidSurface(
                            layerBackdrop,
                            Modifier.widthIn(max = (containerWidth - 160.dp).coerceAtLeast(0.dp))
                        ) {
                            Row(
                                Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    chatInfo.title,
                                    style = MiuixTheme.textStyles.footnote1,
                                    fontWeight = FontWeight.Bold,
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
                verticalAlignment = Alignment.Bottom
            ) {
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
                                tdString("ChatLocation") to SFIcons.Mappin_And_Ellipse
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
                        }) {
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
                                    exit = fadeOut() + slideOutHorizontally { it }) {
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
                                        })
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
            LocalOnDownloadVideo provides { messageId: Long -> viewModel.downloadVideo(messageId) }) {
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
                verticalArrangement = Arrangement.Top,
                overscrollEffect = null,
            ) {
                if (state.loading && state.messages.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tdString("Loading"),
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                textAlign = TextAlign.Center,
                                style = MiuixTheme.textStyles.footnote1
                            )
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
                        key = { state.chatItems[it].key },
                        contentType = { state.chatItems[it]::class.simpleName }) { index ->
                        when (val item = state.chatItems[index]) {
                            is TimestampItem -> {
                                TimestampLabel(timestamp = item.timestamp)
                            }

                            is MessageItem -> {
                                val primaryMessage = item.primaryMessage
                                if (primaryMessage.messageType == MessageType.SYSTEM) {
                                    SystemMessage(primaryMessage)
                                }
                                MessageBubble(
                                    message = primaryMessage,
                                    groupPosition = item.groupPosition,
                                    showAvatar = state.chatInfo?.type == ChatType.GROUP,
                                    albumMessages = if (item.isAlbum) item.messages else null,
                                    onReplyClick = onReplyClick,
                                )
                            }
                        }
                    }

                    if (state.loadingMore) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tdString("Loading"),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    textAlign = TextAlign.Center,
                                    style = MiuixTheme.textStyles.footnote1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
