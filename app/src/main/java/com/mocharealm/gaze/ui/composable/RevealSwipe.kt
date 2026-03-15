package com.mocharealm.gaze.ui.composable

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

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
            val offset = state.safeOffset // 只有在闭包内读取，Compose 才能追踪状态变化！
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
        // 1. 背景层 (滑动菜单)
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

enum class RevealDirection {
    StartToEnd,
    EndToStart
}

enum class RevealValue {
    Default,
    FullyRevealedEnd,
    FullyRevealedStart,
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
        RevealState(
            maxRevealDp = maxRevealDp,
            directions = directions,
            density = density,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold
        )
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
        snapAnimationSpec = tween(),
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