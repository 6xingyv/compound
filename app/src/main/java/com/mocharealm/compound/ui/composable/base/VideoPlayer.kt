package com.mocharealm.compound.ui.composable.base

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    filePath: String,
    modifier: Modifier = Modifier,
    playerSurfaceModifier: Modifier = Modifier,
    loop: Boolean = true,
    mute: Boolean = true,
    playWhenReady: Boolean = true,
    gestureHandler: PointerInputEventHandler = {},
    useTextureView: Boolean = true,
    playerControls: @Composable BoxScope.(player: ExoPlayer) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (filePath.startsWith("http")) {
                Uri.parse(filePath)
            } else {
                Uri.fromFile(File(filePath))
            }
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            this.playWhenReady = playWhenReady
            volume = if (mute) 0f else 1f
            prepare()
        }
    }

    DisposableEffect(filePath) {
        onDispose { exoPlayer.release() }
    }

    Box(modifier = modifier) {
        PlayerSurface(
            player = exoPlayer,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit, gestureHandler)
                .then(playerSurfaceModifier),
            surfaceType = if (useTextureView) SURFACE_TYPE_TEXTURE_VIEW else SURFACE_TYPE_SURFACE_VIEW
        )

        playerControls(exoPlayer)
    }
}