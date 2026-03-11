package com.mocharealm.compound.ui.screen.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mocharealm.compound.ui.composable.base.VideoPlayer
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.MediaItem
import com.mocharealm.compound.ui.util.LocalAnimatedVisibilityScope
import com.mocharealm.compound.ui.util.LocalSharedTransitionScope
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.nav.LocalListDetailExpanded
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File

@Composable
fun MediaPreviewScreen(items: List<MediaItem>, initialIndex: Int) {
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
        expandedState?.value = true
        onDispose { expandedState?.value = originalValue }
    }

    LaunchedEffect(pagerState.currentPage, screenWidthPx) {
        if (screenWidthPx > 0) {
            listState.animateScrollToItem(pagerState.currentPage, 0)
        }
    }

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val layerBackdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { screenWidthPx = it.size.width }
    ) {
        var isPagerEnabled by remember { mutableStateOf(true) }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().layerBackdrop(layerBackdrop),
            pageSpacing = 16.dp,
            userScrollEnabled = isPagerEnabled
        ) { page ->
            val item = items[page]
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val mediaModifier = Modifier.fillMaxSize()
                val finalModifier =
                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            mediaModifier.sharedElement(
                                rememberSharedContentState(key = "media_${item.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    } else mediaModifier

                if (item.type == MediaItem.MediaType.VIDEO && item.url.isNotEmpty()) {
                    AdvancedVideoPlayer(
                        filePath = item.url,
                        modifier = finalModifier,
                        layerBackdrop = layerBackdrop
                    )
                } else {
                    ZoomableImage(
                        url = item.url,
                        thumbnailUrl = item.thumbnailUrl,
                        modifier = finalModifier,
                        onZoomStateChanged = { zoomed -> isPagerEnabled = !zoomed }
                    )
                }
            }
        }

        // Top Bar
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                with(animatedVisibilityScope) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                            .renderInSharedTransitionScopeOverlay()
                            .animateEnterExit(enter = fadeIn(), exit = fadeOut())
                            .zIndex(10f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LiquidSurface(
                            layerBackdrop,
                            Modifier.size(48.dp),
                            Modifier.clickable { navigator.pop() },
                            effects = {
                                vibrancy(); blur(1.dp.toPx()); lens(
                                16.dp.toPx(),
                                32.dp.toPx()
                            )
                            }
                        ) {
                            Icon(
                                SFIcons.Chevron_Backward,
                                null,
                                Modifier.align(Alignment.Center),
                                Color.White
                            )
                        }

                        LiquidSurface(
                            layerBackdrop, Modifier, isInteractive = false,
                            effects = { vibrancy(); blur(1.dp.toPx()) },
                            shape = { ContinuousRoundedRectangle(24.dp) }
                        ) {
                            Text(
                                text = tdString("Of","%1\$d" to pagerState.currentPage + 1, "%2\$d" to items.size),
                                color = Color.White,
                                style = MiuixTheme.textStyles.body2,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.size(48.dp))
                    }

                    // Bottom Bar (Thumbnails)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .fillMaxWidth()
                            .height(72.dp)
                            .renderInSharedTransitionScopeOverlay()
                            .animateEnterExit(enter = fadeIn(), exit = fadeOut())
                            .zIndex(10f)
                    ) {
                        val itemWidthPx = with(density) { thumbnailWidth.toPx() }
                        val contentPadding = with(density) { (screenWidthPx / 2f - itemWidthPx / 2f).toDp() }.coerceAtLeast(0.dp)

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
                                        .size(size)
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
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(
                                                if ((item.thumbnailUrl
                                                        ?: item.url).startsWith("http")
                                                ) (item.thumbnailUrl ?: item.url) else File(
                                                    item.thumbnailUrl ?: item.url
                                                )
                                            )
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
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
}

@Composable
fun ZoomableImage(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onZoomStateChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
        onZoomStateChanged(scale > 1f)
    }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                        }
                        onZoomStateChanged(scale > 1f)
                    }
                )
            }
            .transformable(state = transformState)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(if (url.startsWith("http")) url else if (url.isNotEmpty()) File(url) else File(thumbnailUrl ?: ""))
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun AdvancedVideoPlayer(
    filePath: String,
    modifier: Modifier = Modifier,
    layerBackdrop: com.mocharealm.gaze.glassy.liquid.effect.backdrops.LayerBackdrop
) {
    var isControlsVisible by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isLongPressing by remember { mutableStateOf(false) }
    var longPressDirection by remember { mutableStateOf(0) } // -1 for left, 1 for right

    VideoPlayer(
        filePath = filePath,
        modifier = modifier,
        loop = true,
        mute = false,
        playWhenReady = true,
        gestureHandler = {
            detectTapGestures(
                onTap = { isControlsVisible = !isControlsVisible },
                onLongPress = { offset ->
                    isLongPressing = true
                    longPressDirection = if (offset.x < size.width / 2) -1 else 1
                },
                onPress = {
                    tryAwaitRelease()
                    isLongPressing = false
                    longPressDirection = 0
                }
            )
        },
        playerControls = { player ->
            LaunchedEffect(isLongPressing, longPressDirection) {
                if (isLongPressing) {
                    val targetSpeed = if (longPressDirection == 1) 2f else 0.5f
                    player.setPlaybackSpeed(targetSpeed)
                    while (isLongPressing) {
                        if (longPressDirection == -1) {
                            player.seekTo(maxOf(0, player.currentPosition - 500))
                        }
                        delay(100)
                    }
                    player.setPlaybackSpeed(playbackSpeed)
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
        }
    )
}

@Composable
fun BoxScope.VideoControlOverlay(
    player: ExoPlayer,
    isVisible: Boolean,
    layerBackdrop: com.mocharealm.gaze.glassy.liquid.effect.backdrops.LayerBackdrop,
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
        duration = player.duration.coerceAtLeast(0L)
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0L)
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, isPlaying) {
        while (true) {
            position = player.currentPosition
            delay(500)
        }
    }

    // Fast Forward/Backward visual indicator
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
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            // Central Play/Pause
            LiquidSurface(
                layerBackdrop,
                Modifier
                    .size(64.dp)
                    .align(Alignment.Center),
                Modifier.clickable { if (player.isPlaying) player.pause() else player.play() },
                effects = { vibrancy(); lens(16.dp.toPx(), 32.dp.toPx()) },
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

            // Bottom Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Seek Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(position), color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                        onValueChange = { player.seekTo((it * duration).toLong()) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text(formatTime(duration), color = Color.White, fontSize = 12.sp)
                }

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Speed Button
                    Text(
                        text = "${playbackSpeed}x",
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(0.1f))
                            .clickable {
                                val nextSpeed = when (playbackSpeed) {
                                    1f -> 1.5f
                                    1.5f -> 2f
                                    2f -> 0.5f
                                    else -> 1f
                                }
                                onSpeedChange(nextSpeed)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    // Rewind 15s
                    Icon(
                        SFIcons.`15_Arrow_Trianglehead_Counterclockwise`, null,
                        Modifier
                            .size(32.dp)
                            .clickable { player.seekTo(maxOf(0, player.currentPosition - 15000)) },
                        Color.White
                    )

                    // Forward 15s
                    Icon(
                        SFIcons.`15_Arrow_Trianglehead_Clockwise`, null,
                        Modifier
                            .size(32.dp)
                            .clickable {
                                player.seekTo(
                                    minOf(
                                        duration,
                                        player.currentPosition + 15000
                                    )
                                )
                            },
                        Color.White
                    )
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
