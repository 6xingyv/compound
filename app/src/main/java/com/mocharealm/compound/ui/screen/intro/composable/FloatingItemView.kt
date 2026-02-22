package com.mocharealm.compound.ui.screen.intro.composable

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 动画阶段
 */
enum class AnimationPhase {
    INITIAL_FADE_IN,  // 初始淡入阶段
    LOOPING           // 循环阶段
}

/**
 * 状态容器：负责单个 Item 的动画逻辑与复用标记
 */
@Stable
class FloatingItemState(
    val id: Int,
    initialY: Float,
    val initialXPercent: Float,
    val z: Float,
    val speed: Float,
    val rotation: Float
) {
    var animatableY = Animatable(initialY)
    var animatableAlpha = Animatable(0f)
    var isVisible by mutableStateOf(true)
    var contentIndex by mutableIntStateOf(0)
    var phase by mutableStateOf(AnimationPhase.INITIAL_FADE_IN)
    val initialYPosition = initialY

    // 目标位置（用于计算速度）
    var targetY by mutableFloatStateOf(-1500f)
}

fun Random.nextGaussian(): Double {
    val u1 = nextDouble()
    val u2 = nextDouble()
    return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
}

@Composable
fun ContinuousDepthFloatingScene(
    children: List<@Composable () -> Unit>,
    modifier: Modifier = Modifier,
    itemCount: Int = 15
) {
    if (children.isEmpty()) return

    val density = LocalDensity.current
    val screenHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()

    val pool = remember {
        List(itemCount) { i ->
            val mean = 0.15f
            val stdDev = 0.15f
            val gaussianX = (Random.nextGaussian().toFloat() * stdDev) + mean
            val finalX = gaussianX.coerceIn(-0.5f, 1.2f)

            // 随机决定初始Y位置
            val initialY = if (Random.nextFloat() > 0.3f) {
                Random.nextFloat() * screenHeightPx
            } else {
                if (Random.nextBoolean()) {
                    -Random.nextFloat() * screenHeightPx
                } else {
                    screenHeightPx + Random.nextFloat() * screenHeightPx
                }
            }

            FloatingItemState(
                id = i,
                initialY = initialY,
                initialXPercent = finalX,
                z = Random.nextFloat(),
                speed = 100f + Random.nextFloat() * 150f,
                rotation = Random.nextFloat() * 8f - 4f
            ).apply {
                contentIndex = i % children.size
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        pool.forEach { state ->
            key(state.id) {
                // 统一的动画控制器
                LaunchedEffect(Unit) {
                    var lastVelocity = 0f

                    while (true) {
                        when (state.phase) {
                            AnimationPhase.INITIAL_FADE_IN -> {
                                // 随机延迟
                                val delay = Random.nextLong(0, 500)
                                delay(delay)

                                // 计算初始fadeIn的目标位置（上浮100dp）
                                val fadeInTargetY = state.initialYPosition - with(density) { 100.dp.toPx() }

                                // 同时执行淡入和上浮，但使用相同的动画规格确保速度连续
                                launch {
                                    state.animatableAlpha.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(
                                            durationMillis = 1000,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }

                                // 记录起始速度（如果没有速度就使用0）
                                val startVelocity = lastVelocity

                                // 上浮动画 - 使用基于速度的动画规格
                                state.animatableY.animateTo(
                                    targetValue = fadeInTargetY,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = Spring.StiffnessMedium,
                                        visibilityThreshold = 1f
                                    )
                                )

                                // 记录结束速度
                                lastVelocity = state.animatableY.velocity

                                // 切换到循环阶段
                                state.phase = AnimationPhase.LOOPING
                                state.targetY = -1500f
                            }

                            AnimationPhase.LOOPING -> {
                                // 计算到目标位置的剩余距离
                                val remainingDistance = state.targetY - state.animatableY.value

                                // 根据距离动态调整动画参数
                                val duration = when {
                                    abs(remainingDistance) > screenHeightPx -> 3000  // 长距离慢速
                                    abs(remainingDistance) > screenHeightPx / 2 -> 2000
                                    else -> 1000
                                }

                                // 使用匀速+缓出的组合，保持速度连续
                                state.animatableY.animateTo(
                                    targetValue = state.targetY,
                                    animationSpec = keyframes {
                                        durationMillis = duration
                                        state.targetY at duration with LinearEasing
                                    }
                                )

                                // 到达目标位置，准备重置
                                if (state.animatableY.value <= -1500f) {
                                    // 淡出
                                    state.animatableAlpha.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(300)
                                    )

                                    state.isVisible = false
                                    state.contentIndex = (state.contentIndex + itemCount) % children.size

                                    // 重置位置到底部
                                    val nextResetY = screenHeightPx + (Random.nextFloat() * 200f)
                                    state.animatableY.snapTo(nextResetY)

                                    // 设置新的目标位置（继续向上）
                                    state.targetY = -1500f

                                    // 淡入 - 使用弹簧动画保持速度连续
                                    state.animatableAlpha.snapTo(0f)
                                    state.isVisible = true

                                    state.animatableAlpha.animateTo(
                                        targetValue = 1f,
                                        animationSpec = spring(
                                            dampingRatio = 0.6f,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (state.isVisible) {
                    ZMappedFloatingItem(
                        state = state,
                        screenHeightPx = screenHeightPx,
                        screenWidthPx = screenWidthPx,
                        content = children[state.contentIndex]
                    )
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
    val density = LocalDensity.current
    val z = state.z
    val focusZ = 0.5f

    val scale = 0.7f + (z * 0.6f)
    val distanceToFocus = abs(z - focusZ)
    val blurRadius = with(density) { (distanceToFocus * 4).dp.toPx() }

    val effect = remember(blurRadius) {
        if (blurRadius < 1f) null else {
            val blur = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.DECAL)
            val alphaMatrix = ColorMatrix(floatArrayOf(
                1f,0f,0f,0f,0f,
                0f,1f,0f,0f,0f,
                0f,0f,1f,0f,0f,
                0f,0f,0f,1.5f,0f
            ))
            RenderEffect.createChainEffect(
                RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(alphaMatrix)),
                blur
            ).asComposeRenderEffect()
        }
    }

    // 根据动画阶段微调alpha（让循环阶段的淡入淡出更自然）
    val finalAlpha = state.animatableAlpha.value * when (state.phase) {
        AnimationPhase.INITIAL_FADE_IN -> 1f
        AnimationPhase.LOOPING -> {
            // 在循环阶段，根据Y位置微调透明度，让顶部和底部的item稍微淡出
            val y = state.animatableY.value
            when {
                y < -1000f -> 0.7f  // 接近顶部时稍微淡出
                y > screenHeightPx + 200f -> 0.5f  // 超出底部时淡出
                else -> 1f
            }
        }
    }

    Box(
        modifier = Modifier
            .zIndex(z)
            .graphicsLayer {
                translationX = state.initialXPercent * screenWidthPx
                translationY = state.animatableY.value
                scaleX = scale
                scaleY = scale
                rotationZ = state.rotation
                alpha = finalAlpha
                renderEffect = effect
            }
    ) {
        content()
    }
}