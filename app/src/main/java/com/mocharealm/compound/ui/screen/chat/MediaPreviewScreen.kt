package com.mocharealm.compound.ui.screen.chat

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mocharealm.compound.ui.composable.ManualRollingNumber
import com.mocharealm.compound.ui.composable.base.VideoPlayer
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.MediaItem
import com.mocharealm.compound.ui.util.LocalSharedTransitionScope
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.LayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.drawBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.icons.`15_Arrow_Trianglehead_Clockwise`
import com.mocharealm.gaze.icons.`15_Arrow_Trianglehead_Counterclockwise`
import com.mocharealm.gaze.icons.Chevron_Backward
import com.mocharealm.gaze.icons.Pause_Fill
import com.mocharealm.gaze.icons.Play_Fill
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.nav.LocalListDetailExpanded
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@Composable
fun MediaPreviewScreen(items: List<MediaItem>, initialIndex: Int) {
    val view = LocalView.current
    val navigator = LocalNavigator.current
    val expandedState = LocalListDetailExpanded.current
    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var screenWidthPx by remember { mutableStateOf(0) }
    val thumbnailWidth = 56.dp
    val thumbnailSpacing = 8.dp

    DisposableEffect(Unit) {
        val originalValue = expandedState?.value ?: false

        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        val originalStatusBarSetting = controller.isAppearanceLightStatusBars
        val originalNavigationBarSetting = controller.isAppearanceLightNavigationBars

        expandedState?.value = true

        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false

        onDispose {
            expandedState?.value = originalValue

            controller.isAppearanceLightStatusBars = originalStatusBarSetting
            controller.isAppearanceLightNavigationBars = originalNavigationBarSetting
        }
    }

    LaunchedEffect(pagerState.currentPage, screenWidthPx) {
        if (screenWidthPx > 0) {
            listState.animateScrollToItem(pagerState.currentPage, 0)
        }
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current

    val layerBackdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { screenWidthPx = it.size.width }) {
        var isPagerEnabled by remember { mutableStateOf(true) }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(layerBackdrop),
            pageSpacing = 16.dp,
            userScrollEnabled = isPagerEnabled
        ) { page ->
            val item = items[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val mediaModifier = Modifier.fillMaxSize()
                val finalModifier = with(sharedTransitionScope) {
                    mediaModifier.sharedElement(
                        rememberSharedContentState(key = "media_${item.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }

                if (item.type == MediaItem.MediaType.VIDEO && item.url.isNotEmpty()) {
                    AdvancedVideoPlayer(
                        filePath = item.url,
                        modifier = finalModifier,
                    )
                } else {
                    ZoomableImage(
                        url = item.url,
                        thumbnailUrl = item.thumbnailUrl,
                        modifier = finalModifier,
                        onZoomStateChanged = { zoomed -> isPagerEnabled = !zoomed })
                }
            }
        }

        with(sharedTransitionScope) {
            with(animatedVisibilityScope) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.Black, 1f to Color.Transparent
                                    )
                                )
                            }
                        }
                        .renderInSharedTransitionScopeOverlay(1f)
                        .animateEnterExit(enter = fadeIn(), exit = fadeOut())
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp)
                        .zIndex(10f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    LiquidSurface(
                        layerBackdrop,
                        Modifier.size(48.dp),
                        Modifier.clickable { navigator.pop() },
                        effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx())
                        }) {
                        Icon(
                            SFIcons.Chevron_Backward,
                            null,
                            Modifier.align(Alignment.Center),
                            Color.White
                        )
                    }

                    LiquidSurface(
                        layerBackdrop,
                        Modifier.animateContentSize(),
                        isInteractive = false,
                        shape = { ContinuousRoundedRectangle(24.dp) }) {
                        Text(
                            text = tdString(
                                "Of", "%1\$d" to pagerState.currentPage + 1, "%2\$d" to items.size
                            ),
                            color = Color.White,
                            style = MiuixTheme.textStyles.body2,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.size(48.dp))
                }

                // Bottom Bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .drawWithCache {
                            onDrawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent, 1f to Color.Black
                                    )
                                )
                            }
                        }
                        .renderInSharedTransitionScopeOverlay()
                        .animateEnterExit(enter = fadeIn(), exit = fadeOut())
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp)
                        .fillMaxWidth()
                        .height(72.dp)
                        .zIndex(10f)) {
                    val itemWidthPx = with(density) { thumbnailWidth.toPx() }
                    val contentPadding =
                        with(density) { (screenWidthPx / 2f - itemWidthPx / 2f).toDp() }.coerceAtLeast(
                            0.dp
                        )

                    LazyRow(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = contentPadding),
                        horizontalArrangement = Arrangement.spacedBy(thumbnailSpacing),
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(items) { index, item ->
                            val isSelected = pagerState.currentPage == index
                            val size by animateDpAsState(if (isSelected) 56.dp else 44.dp)

                            Box(
                                modifier = Modifier
                                    .height(size)
                                    .widthIn(30.dp, 56.dp)
                                    .clip(ContinuousRoundedRectangle(12.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = ContinuousRoundedRectangle(12.dp)
                                    )
                                    .clickable {
                                        scope.launch {
                                            pagerState.animateScrollToPage(
                                                index
                                            )
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(
                                        if ((item.thumbnailUrl
                                                ?: item.url).startsWith("http")
                                        ) (item.thumbnailUrl ?: item.url) else File(
                                            item.thumbnailUrl ?: item.url
                                        )
                                    ).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
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
fun ZoomableImage(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onZoomStateChanged: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .onGloballyPositioned { containerSize = it.size }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scope.launch {
                            if (scale.value > 1f) {
                                launch { scale.animateTo(1f) }
                                launch { offsetX.animateTo(0f) }
                                launch { offsetY.animateTo(0f) }
                                onZoomStateChanged(false)
                            } else {
                                val targetScale = 3f
                                val maxOffsetX =
                                    (containerSize.width * targetScale - containerSize.width) / 2f
                                val maxOffsetY =
                                    (containerSize.height * targetScale - containerSize.height) / 2f
                                val destX =
                                    (containerSize.width / 2f - tapOffset.x) * (targetScale - 1f)
                                val destY =
                                    (containerSize.height / 2f - tapOffset.y) * (targetScale - 1f)
                                launch { scale.animateTo(targetScale) }
                                launch {
                                    offsetX.animateTo(
                                        destX.coerceIn(
                                            -maxOffsetX, maxOffsetX
                                        )
                                    )
                                }
                                launch {
                                    offsetY.animateTo(
                                        destY.coerceIn(
                                            -maxOffsetY, maxOffsetY
                                        )
                                    )
                                }
                                onZoomStateChanged(true)
                            }
                        }
                    })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val isMultiTouch = event.changes.size > 1
                        val isZoomed = scale.value > 1.01f

                        if (isMultiTouch || isZoomed) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                event.changes.forEach { it.consume() }
                            }

                            val newScale = (scale.value * zoomChange).coerceIn(1f, 5f)
                            val actualZoom = newScale / scale.value

                            scope.launch {
                                scale.snapTo(newScale)
                                val maxOffsetX =
                                    (containerSize.width * newScale - containerSize.width) / 2f
                                val maxOffsetY =
                                    (containerSize.height * newScale - containerSize.height) / 2f
                                offsetX.snapTo(
                                    (offsetX.value + panChange.x * actualZoom).coerceIn(
                                        -maxOffsetX, maxOffsetX
                                    )
                                )
                                offsetY.snapTo(
                                    (offsetY.value + panChange.y * actualZoom).coerceIn(
                                        -maxOffsetY, maxOffsetY
                                    )
                                )

                                onZoomStateChanged(newScale > 1.01f)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offsetX.value
                translationY = offsetY.value
            }) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(
                if (url.startsWith("http")) url else if (url.isNotEmpty()) File(url) else File(
                    thumbnailUrl ?: ""
                )
            ).build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun AdvancedVideoPlayer(
    filePath: String, modifier: Modifier = Modifier
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressDirection by remember { mutableIntStateOf(0) }

    val layerBackdrop = rememberLayerBackdrop()

    VideoPlayer(
        filePath = filePath,
        modifier = modifier,
        playerSurfaceModifier = Modifier.layerBackdrop(layerBackdrop),
        playWhenReady = true,
        gestureHandler = {
            detectTapGestures(
                onTap = { isControlsVisible = !isControlsVisible },
                onLongPress = { offset ->
                    // 增加安全检查：只有在播放器准备好后才允许长按操作
                    isLongPressing = true
                    longPressDirection = if (offset.x < size.width / 2) -1 else 1
                },
                onPress = {
                    tryAwaitRelease()
                    isLongPressing = false
                    longPressDirection = 0
                })
        },
        playerControls = { player ->
            LaunchedEffect(isLongPressing, longPressDirection) {
                // 确保只有在 READY 状态下才执行 seek 或变速
                if (isLongPressing && player.playbackState == Player.STATE_READY) {
                    val originalSpeed = player.playbackParameters.speed
                    val targetSpeed = if (longPressDirection == 1) 2f else 0.5f

                    player.setPlaybackSpeed(targetSpeed)
                    try {
                        while (isLongPressing) {
                            if (longPressDirection == -1) {
                                player.seekTo(maxOf(0, player.currentPosition - 1000))
                            }
                            delay(200) // 增加延迟，减少底层压力
                        }
                    } finally {
                        player.setPlaybackSpeed(playbackSpeed)
                    }
                }
            }

            VideoControlOverlay(
                player = player,
                isVisible = isControlsVisible,
                layerBackdrop = layerBackdrop,
                playbackSpeed = playbackSpeed,
                onSpeedChange = {
                    playbackSpeed = it
                    player.setPlaybackSpeed(it)
                },
                isLongPressing = isLongPressing,
                longPressDirection = longPressDirection
            )
        })
}

@Composable
fun BoxScope.VideoControlOverlay(
    player: ExoPlayer,
    isVisible: Boolean,
    layerBackdrop: LayerBackdrop,
    playbackSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    isLongPressing: Boolean,
    longPressDirection: Int
) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        isPlaying = player.isPlaying
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0L)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player) {
        while (true) {
            if (player.isPlaying) {
                position = player.currentPosition
            }
            delay(500)
        }
    }
    AnimatedVisibility(
        visible = isLongPressing,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.Center)
    ) {
        Text(
            text = if (longPressDirection == 1) "2X >>" else "<< 0.5X",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(Color.Black.copy(0.4f), CircleShape)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }

    AnimatedVisibility(
        visible = isVisible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    SFIcons.`15_Arrow_Trianglehead_Counterclockwise`,
                    null,
                    Modifier
                        .size(32.dp)
                        .clickable {
                            player.seekTo(
                                maxOf(
                                    0, player.currentPosition - 15000
                                )
                            )
                        },
                    Color.White
                )

                LiquidSurface(
                    layerBackdrop,
                    Modifier.size(64.dp),
                    Modifier.clickable { if (player.isPlaying) player.pause() else player.play() },
                    effects = {
                        vibrancy()
                        lens(16.dp.toPx(), 32.dp.toPx())
                    },
                    surfaceColor = Color.White.copy(0.2f)
                ) {
                    Icon(
                        if (isPlaying) SFIcons.Pause_Fill else SFIcons.Play_Fill,
                        null,
                        Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        Color.White
                    )
                }
                Icon(
                    SFIcons.`15_Arrow_Trianglehead_Clockwise`,
                    null,
                    Modifier
                        .size(32.dp)
                        .clickable {
                            player.seekTo(
                                minOf(
                                    duration, player.currentPosition + 15000
                                )
                            )
                        },
                    Color.White
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ManualRollingNumber(
                        formatTime(position),
                        Color.White,
                        MiuixTheme.textStyles.body1.copy(fontFamily = FontFamily.Monospace)
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .drawBackdrop(
                                layerBackdrop,
                                shape = { ContinuousRoundedRectangle(4.dp) },
                                effects = {
                                    vibrancy()
                                    lens(16.dp.toPx(), 32.dp.toPx(), false)
                                },
                                onDrawFront = {
                                    val width =
                                        size.width * if (duration > 0) (position.toFloat() / duration).coerceIn(
                                            0f, 1f
                                        ) else 0f
                                    drawRect(Color.White, size = size.copy(width))
                                }
                            )
                    )
                    ManualRollingNumber(
                        formatTime(duration),
                        Color.White,
                        MiuixTheme.textStyles.body1.copy(fontFamily = FontFamily.Monospace)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(0.1f))
                            .clickable {
                                val nextSpeed = when (playbackSpeed) {
                                    1f -> 1.5f; 1.5f -> 2f; 2f -> 0.5f; else -> 1f
                                }
                                onSpeedChange(nextSpeed)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
