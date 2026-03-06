package com.mocharealm.compound.ui.composable.base

import android.graphics.SurfaceTexture
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class VpxDecoder(private val filePath: String) {
    private var nativePtr: Long = 0

    init {
        try {
            nativePtr = nativeInit(filePath)
        } catch (e: UnsatisfiedLinkError) {
            Log.e("VpxDecoder", "Native library not loaded", e)
        }
    }

    fun setSurface(surface: Surface?, width: Int = 0, height: Int = 0) {
        if (nativePtr != 0L) {
            nativeSetSurface(nativePtr, surface, width, height)
        }
    }

    fun play(loop: Boolean) {
        if (nativePtr != 0L) {
            nativePlay(nativePtr, loop)
        }
    }

    fun stop() {
        if (nativePtr != 0L) {
            nativeStop(nativePtr)
        }
    }

    fun release() {
        if (nativePtr != 0L) {
            nativeRelease(nativePtr)
            nativePtr = 0
        }
    }

    private external fun nativeInit(filePath: String): Long
    private external fun nativeSetSurface(ptr: Long, surface: Surface?, width: Int, height: Int)
    private external fun nativePlay(ptr: Long, loop: Boolean)
    private external fun nativeStop(ptr: Long)
    private external fun nativeRelease(ptr: Long)

    companion object {
        init {
            try {
                System.loadLibrary("vpxplayer")
            } catch (e: UnsatisfiedLinkError) {
                Log.w("VpxDecoder", "Failed to load libvpxplayer.so. " +
                        "Make sure libvpx & libwebm are compiled and added to CMakeLists!")
            }
        }
    }
}

/**
 * A custom video player that utilizes libvpx internally to decode and render WEBM with Alpha channel 
 * efficiently to a Surface, doing zero-copy YUVA uploading directly to OpenGL ES.
 */
@Composable
fun VpxVideoPlayer(
    filePath: String,
    modifier: Modifier = Modifier,
    loop: Boolean = true
) {
    val decoder = remember(filePath) { VpxDecoder(filePath) }

    DisposableEffect(decoder) {
        onDispose {
            decoder.stop()
            decoder.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    isOpaque = false // Required for transparent background
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(st)
                            decoder.setSurface(surface, width, height)
                            decoder.play(loop)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                            val surface = Surface(st)
                            decoder.setSurface(surface, width, height)
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            decoder.setSurface(null, 0, 0)
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
