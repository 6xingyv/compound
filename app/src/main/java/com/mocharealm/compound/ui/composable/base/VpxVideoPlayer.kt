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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class VpxDecoder(private val filePath: String) {
    private val nativePtr = AtomicLong(0L)
    private val released = AtomicBoolean(false)

    init {
        try {
            nativePtr.set(nativeInit(filePath))
        } catch (e: UnsatisfiedLinkError) {
            Log.e("VpxDecoder", "Native library not loaded", e)
        }
    }

    fun setSurface(surface: Surface?, width: Int = 0, height: Int = 0) {
        val ptr = nativePtr.get()
        if (ptr != 0L && !released.get()) {
            nativeSetSurface(ptr, surface, width, height)
        }
    }

    fun play(loop: Boolean) {
        val ptr = nativePtr.get()
        if (ptr != 0L && !released.get()) {
            nativePlay(ptr, loop)
        }
    }

    fun stop() {
        val ptr = nativePtr.get()
        if (ptr != 0L && !released.get()) {
            nativeStop(ptr)
        }
    }

    fun release() {
        if (released.compareAndSet(false, true)) {
            val ptr = nativePtr.getAndSet(0L)
            if (ptr != 0L) {
                // Async: sets quit flag, detaches decode thread, deletes on background
                nativeRelease(ptr, true)
            }
        }
    }

    private external fun nativeInit(filePath: String): Long
    private external fun nativeSetSurface(ptr: Long, surface: Surface?, width: Int, height: Int)
    private external fun nativePlay(ptr: Long, loop: Boolean)
    private external fun nativeStop(ptr: Long)
    private external fun nativeRelease(ptr: Long, async: Boolean)

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
            // Non-blocking: signals quit and cleans up on a background thread
            decoder.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    isOpaque = false // Required for transparent background
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        private var currentSurface: Surface? = null

                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                            currentSurface?.release()
                            val surface = Surface(st)
                            currentSurface = surface
                            decoder.setSurface(surface, width, height)
                            decoder.play(loop)
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                            // Surface object doesn't change on resize, just update dimensions
                            currentSurface?.let { decoder.setSurface(it, width, height) }
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            decoder.setSurface(null, 0, 0)
                            currentSurface?.release()
                            currentSurface = null
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
