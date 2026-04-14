package com.mocharealm.compound.ui.composable.base

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
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
    contentScale: ContentScale = ContentScale.Fit,
    gestureHandler: PointerInputEventHandler = {},
    useTextureView: Boolean = true,
    playerControls: @Composable BoxScope.(player: ExoPlayer) -> Unit = { _ -> }
) {
    val context = LocalContext.current
    var videoAspectRatio by remember(filePath) { mutableFloatStateOf(16f / 9f) }

    val exoPlayer = remember(filePath) {
        ExoPlayer.Builder(context).build().apply {
            val uri = if (filePath.startsWith("http")) {
                filePath.toUri()
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

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val pixelRatio = if (videoSize.pixelWidthHeightRatio > 0f) {
                        videoSize.pixelWidthHeightRatio
                    } else {
                        1f
                    }
                    val isRotated = (videoSize.unappliedRotationDegrees % 180) != 0
                    val rawAspectRatio = (videoSize.width * pixelRatio) / videoSize.height
                    videoAspectRatio = if (isRotated && rawAspectRatio > 0f) {
                        1f / rawAspectRatio
                    } else {
                        rawAspectRatio
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(filePath) {
        onDispose { exoPlayer.release() }
    }

    BoxWithConstraints(modifier = modifier) {
        val containerAspectRatio = if (maxHeight.value > 0f) {
            maxWidth.value / maxHeight.value
        } else {
            1f
        }

        Box(Modifier.fillMaxSize()) {
            val surfaceModifier = if (contentScale == ContentScale.Fit) {
                if (videoAspectRatio >= containerAspectRatio) {
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(videoAspectRatio)
                } else {
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(videoAspectRatio)
                }
            } else {
                Modifier.fillMaxSize()
            }

            PlayerSurface(
                player = exoPlayer,
                modifier = surfaceModifier
                    .align(Alignment.Center)
                    .pointerInput(Unit, gestureHandler)
                    .then(playerSurfaceModifier),
                surfaceType = if (useTextureView) SURFACE_TYPE_TEXTURE_VIEW else SURFACE_TYPE_SURFACE_VIEW
            )

            playerControls(exoPlayer)
        }
    }
}