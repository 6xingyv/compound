package com.mocharealm.compound.ui.screen.intro.composable

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AnimationPhase {
    INITIAL_FADE_IN,
    LOOPING
}

@Stable
class FloatingItemState(
    val id: Int,
    val z: Float, // 0.0 (远) 到 1.0 (近)
    initialX: Float,
    val initialY: Float
) {
    var x by mutableFloatStateOf(initialX)
    val animatableY = Animatable(initialY)
    val animatableAlpha = Animatable(0f)
    var phase by mutableStateOf(AnimationPhase.INITIAL_FADE_IN)

    // 随机旋转角度，增加生动感
    val rotation = Random.nextFloat() * 20f - 10f

    var itemHeight by mutableFloatStateOf(0f)
    var itemWidth by mutableFloatStateOf(0f)

    // 属性绑定 Z 轴
    val speedFactor = 0.6f + z * 1.4f
    val scale = 0.7f + z * 0.5f

    // 越远越模糊 (Z靠近0时模糊)
    val blurRadius = ((1f - z) * 15f).coerceAtLeast(0f)
}

@Composable
fun ContinuousDepthFloatingScene(
    children: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    itemCount: Int = 12
) {
    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    // 初始化状态
    val states = remember {
        val rows = 4
        val cols = 3
        List(itemCount) { i ->
            val row = i / cols
            val col = i % cols
            val baseGridX = (col.toFloat() / cols) * screenWidthPx
            val baseGridY = (row.toFloat() / rows) * screenHeightPx

            FloatingItemState(
                id = i,
                initialX = baseGridX + Random.nextFloat() * (screenWidthPx / cols),
                initialY = baseGridY + Random.nextFloat() * (screenHeightPx / rows),
                z = Random.nextFloat()
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        states.forEachIndexed { index, state ->
            val content = children[index % children.size]

            ZMappedFloatingItem(
                state = state,
                screenHeightPx = screenHeightPx,
                screenWidthPx = screenWidthPx
            ) {
                Box(modifier = Modifier.onSizeChanged {
                    state.itemWidth = it.width.toFloat()
                    state.itemHeight = it.height.toFloat()
                }) {
                    content()
                }
            }

            LaunchedEffect(state.itemWidth, state.itemHeight) {
                if (state.itemWidth == 0f || state.itemHeight == 0f) return@LaunchedEffect

                if (state.phase == AnimationPhase.INITIAL_FADE_IN) {
                    delay(Random.nextLong(0, 800))
                    val initialTargetY = state.animatableY.value - 150f

                    launch { state.animatableAlpha.animateTo(1f, tween(1200)) }

                    state.animatableY.animateTo(
                        targetValue = initialTargetY,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
                    state.phase = AnimationPhase.LOOPING
                }

                while (isActive) {
                    val targetY = -state.itemHeight - 200f
                    val distance = state.animatableY.value - targetY
                    val duration =
                        (distance / (0.12f * state.speedFactor)).toInt().coerceIn(4000, 12000)

                    // 向上漂浮
                    state.animatableY.animateTo(
                        targetValue = targetY,
                        animationSpec = tween(duration, easing = LinearEasing)
                    )

                    // --- 到底部后的重置逻辑 ---
                    state.animatableAlpha.snapTo(0f)
                    state.x = Random.nextFloat() * screenWidthPx
                    state.animatableY.snapTo(screenHeightPx + state.itemHeight + 200f)

                    // 从底部再次浮现
                    launch { state.animatableAlpha.animateTo(1f, tween(800)) }
                    state.animatableY.animateTo(
                        targetValue = screenHeightPx - 100f,
                        animationSpec = spring(dampingRatio = 0.8f)
                    )
                    // 循环会立即回到 while 顶端开始下一次 linear 漂浮，不会停顿
                }
            }
        }
    }
}

@Composable
fun ZMappedFloatingItem(
    state: FloatingItemState,
    screenHeightPx: Float,
    screenWidthPx: Float,
    content: @Composable () -> Unit
) {
    val effect = remember(state.blurRadius) {
        if (state.blurRadius > 1f) {
            val blur = RenderEffect.createBlurEffect(
                state.blurRadius,
                state.blurRadius,
                Shader.TileMode.DECAL
            )
            val alphaMatrix = ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 1.2f, 0f
                )
            )
            RenderEffect.createChainEffect(
                RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(alphaMatrix)),
                blur
            ).asComposeRenderEffect()
        } else null
    }

    val y = state.animatableY.value
    val edgeAlpha = when {
        y < 0f -> (1f + (y / 200f)).coerceIn(0f, 1f)
        y > screenHeightPx -> (1f - ((y - screenHeightPx) / 200f)).coerceIn(0f, 1f)
        else -> 1f
    }

    Box(
        modifier = Modifier
            .zIndex(state.z)
            .graphicsLayer {
                translationX = state.x - (state.itemWidth / 2)
                translationY = state.animatableY.value
                scaleX = state.scale
                scaleY = state.scale
//                rotationZ = state.rotation
                alpha = state.animatableAlpha.value * edgeAlpha
                renderEffect = effect
            }
    ) {
        content()
    }
}