package com.mocharealm.gaze.ui.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

enum class RevealDirection {
    StartToEnd,
    EndToStart
}

@Composable
fun ElasticRevealSwipe(
    shape: CornerBasedShape,
    modifier: Modifier = Modifier,
    state: ElasticRevealState = rememberElasticRevealState(),
    enabled: Boolean = true,
    onTrigger: (RevealDirection) -> Unit = {},
    swipe: @Composable BoxScope.(direction: RevealDirection?, progress: Float) -> Unit,
    content: @Composable BoxScope.(shape: Shape, progress: Float) -> Unit
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var shapeSize by remember { mutableStateOf(Size.Zero) }

    val offset = state.safeOffset
    val progress by remember(state, density) {
        derivedStateOf {
            val maxPx = with(density) { state.maxRevealDp.toPx() }
            (offset.absoluteValue / maxPx).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
        }
    }

    val currentDirection = remember(offset) {
        when {
            offset > 0f -> RevealDirection.StartToEnd
            offset < 0f -> RevealDirection.EndToStart
            else -> null
        }
    }

    // 动态形状计算
    val animatedShape by remember(shape, shapeSize, density, state, currentDirection) {
        derivedStateOf {
            if (shapeSize == Size.Zero || currentDirection == null) return@derivedStateOf shape
            val cornerRadiusBottomEnd = shape.bottomEnd.toPx(shapeSize, density)
            val cornerRadiusTopEnd = shape.topEnd.toPx(shapeSize, density)
            val cornerRadiusBottomStart = shape.bottomStart.toPx(shapeSize, density)
            val cornerRadiusTopStart = shape.topStart.toPx(shapeSize, density)
            val minDragAmount = maxOf(cornerRadiusTopEnd, cornerRadiusBottomEnd, cornerRadiusTopStart, cornerRadiusBottomStart)
            if (minDragAmount == 0f) return@derivedStateOf shape

            val factor = (offset.absoluteValue / minDragAmount).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
            when (currentDirection) {
                RevealDirection.StartToEnd -> shape.copy(
                    bottomStart = CornerSize(lerp(cornerRadiusBottomStart, 0f, factor)),
                    topStart = CornerSize(lerp(cornerRadiusTopStart, 0f, factor))
                )
                RevealDirection.EndToStart -> shape.copy(
                    bottomEnd = CornerSize(lerp(cornerRadiusBottomEnd, 0f, factor)),
                    topEnd = CornerSize(lerp(cornerRadiusTopEnd, 0f, factor))
                )
            }
        }
    }

    // 无状态的拖拽跟踪，带有橡皮筋效果
    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            val currentOffset = state.offset.value
            val maxPx = with(density) { state.maxRevealDp.toPx() }

            // 弹性阻尼：拉得越远，阻力越大
            val resistance = if (currentOffset.absoluteValue > maxPx) 0.35f else 1f
            val actualDelta = if (isRtl) -delta else delta
            val newOffset = currentOffset + (actualDelta * resistance)

            val clampedOffset = when {
                RevealDirection.StartToEnd in state.directions && RevealDirection.EndToStart in state.directions -> newOffset
                RevealDirection.StartToEnd in state.directions -> newOffset.coerceAtLeast(0f)
                RevealDirection.EndToStart in state.directions -> newOffset.coerceAtMost(0f)
                else -> 0f
            }
            state.offset.snapTo(clampedOffset)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .onSizeChanged { shapeSize = it.toSize() }
    ) {
        // 背景层
        Box(modifier = Modifier.matchParentSize()) {
            if (currentDirection != null) {
                val alignment = when (currentDirection) {
                    RevealDirection.StartToEnd -> Alignment.CenterStart
                    RevealDirection.EndToStart -> Alignment.CenterEnd
                }
                Box(
                    modifier = Modifier
                        .width(state.maxRevealDp)
                        .fillMaxHeight()
                        .align(alignment),
                    contentAlignment = Alignment.Center
                ) {
                    swipe(currentDirection, progress)
                }
            }
        }

        // 前景层
        Box(
            modifier = Modifier
                .offset { IntOffset(x = state.safeOffset.roundToInt(), y = 0) }
                .then(
                    if (enabled) {
                        Modifier.draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = {
                                val maxPx = with(density) { state.maxRevealDp.toPx() }
                                val triggerThreshold = maxPx * state.triggerThresholdRatio

                                // 检查是否达到触发阈值
                                if (state.offset.value <= -triggerThreshold && RevealDirection.EndToStart in state.directions) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTrigger(RevealDirection.EndToStart)
                                } else if (state.offset.value >= triggerThreshold && RevealDirection.StartToEnd in state.directions) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTrigger(RevealDirection.StartToEnd)
                                }

                                // 无论如何都弹回原位
                                coroutineScope.launch { state.resetAnimated() }
                            }
                        )
                    } else Modifier
                )
        ) {
            content(animatedShape, progress)
        }
    }
}

@Composable
fun rememberElasticRevealState(
    maxRevealDp: Dp = 64.dp, // 弹性滑动不需要拉太长
    directions: Set<RevealDirection> = setOf(RevealDirection.EndToStart),
    triggerThresholdRatio: Float = 0.8f
): ElasticRevealState {
    return remember(maxRevealDp, directions, triggerThresholdRatio) {
        ElasticRevealState(maxRevealDp, directions, triggerThresholdRatio)
    }
}

class ElasticRevealState(
    val maxRevealDp: Dp = 64.dp,
    val directions: Set<RevealDirection>,
    val triggerThresholdRatio: Float = 0.8f
) {
    val offset = Animatable(0f)
    val safeOffset: Float get() = offset.value

    suspend fun resetAnimated() {
        offset.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    suspend fun reset() = offset.snapTo(0f)
}

enum class RevealValue {
    Default,
    FullyRevealedEnd,
    FullyRevealedStart,
}

@Composable
fun RevealSwipe(
    shape: CornerBasedShape,
    modifier: Modifier = Modifier,
    state: RevealState = rememberRevealState(),
    enabled: Boolean = true,
    swipe: @Composable BoxScope.(direction: RevealDirection?, progress: Float) -> Unit,
    content: @Composable BoxScope.(shape: Shape, progress: Float) -> Unit
) {
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var shapeSize by remember { mutableStateOf(Size.Zero) }

    val offset = state.safeOffset
    val progress by remember(state, density) {
        derivedStateOf {
            val maxPx = with(density) { state.maxRevealDp.toPx() }
            (offset.absoluteValue / maxPx).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
        }
    }

    val currentDirection = remember(offset) {
        when {
            offset > 0f -> RevealDirection.StartToEnd
            offset < 0f -> RevealDirection.EndToStart
            else -> null
        }
    }

    val animatedShape by remember(shape, shapeSize, density, state, currentDirection) {
        derivedStateOf {
            if (shapeSize == Size.Zero || currentDirection == null) return@derivedStateOf shape
            val cornerRadiusBottomEnd = shape.bottomEnd.toPx(shapeSize, density)
            val cornerRadiusTopEnd = shape.topEnd.toPx(shapeSize, density)
            val cornerRadiusBottomStart = shape.bottomStart.toPx(shapeSize, density)
            val cornerRadiusTopStart = shape.topStart.toPx(shapeSize, density)
            val minDragAmount = maxOf(cornerRadiusTopEnd, cornerRadiusBottomEnd, cornerRadiusTopStart, cornerRadiusBottomStart)
            if (minDragAmount == 0f) return@derivedStateOf shape

            val factor = (offset.absoluteValue / minDragAmount).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
            when (currentDirection) {
                RevealDirection.StartToEnd -> shape.copy(
                    bottomStart = CornerSize(lerp(cornerRadiusBottomStart, 0f, factor)),
                    topStart = CornerSize(lerp(cornerRadiusTopStart, 0f, factor))
                )
                RevealDirection.EndToStart -> shape.copy(
                    bottomEnd = CornerSize(lerp(cornerRadiusBottomEnd, 0f, factor)),
                    topEnd = CornerSize(lerp(cornerRadiusTopEnd, 0f, factor))
                )
            }
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .onSizeChanged { shapeSize = it.toSize() }
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            if (currentDirection != null) {
                val alignment = when (currentDirection) {
                    RevealDirection.StartToEnd -> Alignment.CenterStart
                    RevealDirection.EndToStart -> Alignment.CenterEnd
                }
                Box(
                    modifier = Modifier
                        .width(state.maxRevealDp)
                        .fillMaxHeight()
                        .align(alignment),
                    contentAlignment = Alignment.Center
                ) {
                    swipe(currentDirection, progress)
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(x = state.safeOffset.roundToInt(), y = 0) }
                .then(
                    if (enabled) {
                        Modifier.anchoredDraggable(
                            state = state.anchoredDraggableState,
                            orientation = Orientation.Horizontal,
                            reverseDirection = isRtl
                        )
                    } else Modifier
                )
        ) {
            content(animatedShape, progress)
        }
    }
}

@Composable
fun rememberRevealState(
    key: Any? = Unit,
    maxRevealDp: Dp = 80.dp,
    directions: Set<RevealDirection> = setOf(RevealDirection.StartToEnd, RevealDirection.EndToStart),
    positionalThreshold: (totalDistance: Float) -> Float = { distance -> distance * 0.9f },
    velocityThreshold: Dp = 2500.dp
): RevealState {
    val density = LocalDensity.current
    return remember(key, maxRevealDp, directions, velocityThreshold) {
        RevealState(maxRevealDp, directions, density, positionalThreshold, velocityThreshold)
    }
}

class RevealState(
    val maxRevealDp: Dp = 80.dp,
    val directions: Set<RevealDirection>,
    private val density: Density,
    val positionalThreshold: (totalDistance: Float) -> Float,
    val velocityThreshold: Dp = 2500.dp,
    initialValue: RevealValue = RevealValue.Default,
) {
    val anchoredDraggableState = AnchoredDraggableState(
        initialValue = initialValue,
        anchors = DraggableAnchors {
            RevealValue.Default at 0f
            if (RevealDirection.StartToEnd in directions) {
                RevealValue.FullyRevealedEnd at with(density) { maxRevealDp.toPx() }
            }
            if (RevealDirection.EndToStart in directions) {
                RevealValue.FullyRevealedStart at -with(density) { maxRevealDp.toPx() }
            }
        },
        positionalThreshold = positionalThreshold,
        velocityThreshold = { with(density) { velocityThreshold.toPx() } },
        snapAnimationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy ,
            stiffness = Spring.StiffnessMediumLow
        ),
        decayAnimationSpec = exponentialDecay()
    )

    val safeOffset: Float
        get() = try {
            val offset = anchoredDraggableState.offset
            if (offset.isNaN()) 0f else offset
        } catch (e: IllegalStateException) {
            0f
        }
}

suspend fun RevealState.resetAnimated() {
    anchoredDraggableState.animateTo(targetValue = RevealValue.Default)
}

suspend fun RevealState.reset() {
    anchoredDraggableState.snapTo(targetValue = RevealValue.Default)
}