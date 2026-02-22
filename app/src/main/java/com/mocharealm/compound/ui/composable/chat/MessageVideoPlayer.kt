package com.mocharealm.compound.ui.composable.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mocharealm.compound.ui.composable.base.VideoPlayer
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.LayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.LiquidSurface
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun MessageVideoPlayer(filePath: String, modifier: Modifier = Modifier) {
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
        })
}

@Composable
fun BoxScope.ControlLayer(
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
            layerBackdrop,
            Modifier.size(48.dp),
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
                Icon(
                    if (playing) SFIcons.Pause_Fill else SFIcons.Play_Fill,
                    null,
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
        }
    }
}