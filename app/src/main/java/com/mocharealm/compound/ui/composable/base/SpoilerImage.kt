package com.mocharealm.compound.ui.composable.base

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.ui.util.SpoilerShader
import kotlinx.coroutines.launch
import kotlin.math.hypot


@Composable
fun SpoilerImage(
    modifier: Modifier = Modifier, hasSpoiler: Boolean, content: @Composable () -> Unit
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
                            ), center = origin, radius = radius, blendMode = BlendMode.DstOut
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