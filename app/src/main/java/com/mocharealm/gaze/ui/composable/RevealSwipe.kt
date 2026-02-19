/**
 * I got this spark from [ch4rl3x's RevealSwipe](https://github.com/ch4rl3x/RevealSwipe),
 * and improved it.
 */

package com.mocharealm.gaze.ui.composable

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun RevealSwipe(
    modifier: Modifier = Modifier,
    enableSwipe: Boolean = true,
    onContentClick: (() -> Unit)? = null,
    onContentLongClick: ((DpOffset) -> Unit)? = null,
    backgroundStartActionLabel: String? = null,
    onBackgroundStartClick: () -> Boolean = { true },
    backgroundEndActionLabel: String? = null,
    onBackgroundEndClick: () -> Boolean = { true },
    closeOnContentClick: Boolean = true,
    closeOnBackgroundClick: Boolean = true,
    shape: CornerBasedShape,
    alphaEasing: Easing = CubicBezierEasing(0.4f, 0.4f, 0.17f, 0.9f),
    backgroundCardStartColor: Color,
    backgroundCardEndColor: Color,
    card: @Composable BoxScope.(
        shape: Shape,
        content: @Composable ColumnScope.() -> Unit
    ) -> Unit,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    state: RevealState = rememberRevealState(),
    hiddenContentEnd: @Composable BoxScope.() -> Unit = {},
    hiddenContentStart: @Composable BoxScope.() -> Unit = {},
    content: @Composable (Shape) -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    val closeAction = remember(coroutineScope, state) {
        {
            coroutineScope.launch { state.resetAnimated() }
            Unit
        }
    }

    val handleBackgroundStartClick = remember(closeAction, onBackgroundStartClick, closeOnBackgroundClick) {
        {
            if (closeOnBackgroundClick) closeAction()
            onBackgroundStartClick()
            Unit
        }
    }

    val handleBackgroundEndClick = remember(closeAction, onBackgroundEndClick, closeOnBackgroundClick) {
        {
            if (closeOnBackgroundClick) closeAction()
            onBackgroundEndClick()
            Unit
        }
    }

    // 状态派生，判断是否已展开（用于无障碍语义和点击拦截）
    val isOpen by remember {
        derivedStateOf { state.anchoredDraggableState.targetValue != RevealValue.Default }
    }

    BaseRevealSwipe(
        modifier = modifier.semantics {
            stateDescription = if (isOpen) "Menu Opened" else "Menu Closed"
            customActions = buildList {
                backgroundStartActionLabel?.let { add(CustomAccessibilityAction(it) { onBackgroundStartClick() }) }
                backgroundEndActionLabel?.let { add(CustomAccessibilityAction(it) { onBackgroundEndClick() }) }
            }
        },
        enableSwipe = enableSwipe,
        animateBackgroundCardColor = enableSwipe,
        shape = shape,
        alphaEasing = alphaEasing,
        backgroundCardStartColor = backgroundCardStartColor,
        backgroundCardEndColor = backgroundCardEndColor,
        card = card,
        state = state,
        hiddenContentEnd = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = handleBackgroundEndClick),
                contentAlignment = Alignment.Center
            ) { hiddenContentEnd() }
        },
        hiddenContentStart = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = handleBackgroundStartClick),
                contentAlignment = Alignment.Center
            ) { hiddenContentStart() }
        },
        content = { animatedShape ->
            val interactionSource = remember { MutableInteractionSource() }

            val clickableModifier = when {
                onContentClick != null || closeOnContentClick -> {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        // 如果菜单打开，取消涟漪效果
                        indication = if (isOpen) null else LocalIndication.current,
                        onClick = {
                            if (isOpen && closeOnContentClick) {
                                closeAction()
                            } else {
                                onContentClick?.invoke()
                            }
                        },
                        onLongClick = onContentLongClick?.let { longClick ->
                            {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                longClick.invoke(pressOffset)
                            }
                        }
                    )
                }
                else -> Modifier
            }

            Box(
                modifier = clickableModifier.pointerInput(Unit) {
                    kotlinx.coroutines.coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            pressOffset = DpOffset(down.position.x.toDp(), down.position.y.toDp())
                        }
                    }
                }
            ) {
                content(animatedShape)

                // 拦截层：当菜单打开时，阻止底层内容区域的意外手势冲突
                if (isOpen && closeOnContentClick) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = closeAction
                            )
                    )
                }
            }
        }
    )
}

@Composable
fun BaseRevealSwipe(
    modifier: Modifier = Modifier,
    enableSwipe: Boolean = true,
    animateBackgroundCardColor: Boolean = true,
    shape: CornerBasedShape,
    alphaEasing: Easing = CubicBezierEasing(0.4f, 0.4f, 0.17f, 0.9f),
    backgroundCardStartColor: Color,
    backgroundCardEndColor: Color,
    card: @Composable BoxScope.(
        shape: Shape,
        content: @Composable ColumnScope.() -> Unit
    ) -> Unit,
    state: RevealState,
    flingBehavior: FlingBehavior? = AnchoredDraggableDefaults.flingBehavior(
        state = state.anchoredDraggableState,
        positionalThreshold = state.positionalThreshold,
        animationSpec = tween()
    ),
    hiddenContentEnd: @Composable BoxScope.() -> Unit = {},
    hiddenContentStart: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.(Shape) -> Unit
) {
    val density = LocalDensity.current
    var shapeSize by remember { mutableStateOf(Size.Zero) }

    // 使用 derivedStateOf 派生圆角和透明度，避免滑动时整个 Composable 重组
    val animatedShape by remember(shape, shapeSize, density, state) {
        derivedStateOf {
            if (shapeSize == Size.Zero) return@derivedStateOf shape

            val offset = state.safeOffset
            val cornerRadiusBottomEnd = shape.bottomEnd.toPx(shapeSize, density)
            val cornerRadiusTopEnd = shape.topEnd.toPx(shapeSize, density)
            val cornerRadiusBottomStart = shape.bottomStart.toPx(shapeSize, density)
            val cornerRadiusTopStart = shape.topStart.toPx(shapeSize, density)

            val minDragAmountForStraightCorner = maxOf(cornerRadiusTopEnd, cornerRadiusBottomEnd)

            val factorEnd = if (state.directions.contains(RevealDirection.EndToStart)) {
                (-offset / minDragAmountForStraightCorner).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
            } else 0f

            val factorStart = if (state.directions.contains(RevealDirection.StartToEnd)) {
                (offset / minDragAmountForStraightCorner).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
            } else 0f

            shape.copy(
                bottomStart = CornerSize(lerp(cornerRadiusBottomStart, 0f, factorStart)),
                bottomEnd = CornerSize(lerp(cornerRadiusBottomEnd, 0f, factorEnd)),
                topStart = CornerSize(lerp(cornerRadiusTopStart, 0f, factorStart)),
                topEnd = CornerSize(lerp(cornerRadiusTopEnd, 0f, factorEnd))
            )
        }
    }

    val draggedRatio by remember(state) {
        derivedStateOf {
            val maxPx = with(density) { state.maxRevealDp.toPx() }
            (state.safeOffset.absoluteValue / maxPx).coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }
        }
    }

    Box(
        modifier = modifier.onSizeChanged { shapeSize = it.toSize() }
    ) {
        // 背景层，通过 graphicsLayer 优化 Alpha 动画
        card(shape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (animateBackgroundCardColor) alphaEasing.transform(draggedRatio) else 1f
                    }
            ) {
                val hasStartContent = state.directions.contains(RevealDirection.StartToEnd)
                val hasEndContent = state.directions.contains(RevealDirection.EndToStart)

                if (hasStartContent) {
                    Box(
                        modifier = Modifier
                            .width(state.maxRevealDp)
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .background(backgroundCardStartColor),
                        content = hiddenContentStart
                    )
                }
                if (hasEndContent) {
                    Box(
                        modifier = Modifier
                            .width(state.maxRevealDp)
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .background(backgroundCardEndColor),
                        content = hiddenContentEnd
                    )
                }
            }
        }

        // 前景内容区，利用 Modifier.offset (lambda 形式) 在布局阶段处理位移，避免重组
        Box(
            modifier = Modifier
                .then(
                    if (enableSwipe) {
                        Modifier
                            .offset {
                                IntOffset(
                                    x = state.safeOffset.roundToInt(),
                                    y = 0
                                )
                            }
                            .anchoredDraggable(
                                state = state.anchoredDraggableState,
                                orientation = Orientation.Horizontal,
                                enabled = true,
                                reverseDirection = LocalLayoutDirection.current == LayoutDirection.Rtl,
                                flingBehavior = flingBehavior
                            )
                    } else Modifier
                )
        ) {
            content(animatedShape)
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
    maxRevealDp: Dp = 75.dp,
    directions: Set<RevealDirection> = setOf(RevealDirection.StartToEnd, RevealDirection.EndToStart),
    positionalThreshold: (totalDistance: Float) -> Float = { distance -> distance * 0.5f }
): RevealState {
    val density = LocalDensity.current
    return remember(key,maxRevealDp, directions) {
        RevealState(
            maxRevealDp = maxRevealDp,
            directions = directions,
            density = density,
            positionalThreshold = positionalThreshold
        )
    }
}

class RevealState(
    val maxRevealDp: Dp = 75.dp,
    val directions: Set<RevealDirection>,
    private val density: Density,
    val positionalThreshold: (totalDistance: Float) -> Float,
    initialValue: RevealValue = RevealValue.Default,
) {
    val anchoredDraggableState: AnchoredDraggableState<RevealValue> = AnchoredDraggableState(
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
        velocityThreshold = { with(density) { 125.dp.toPx() } },
        snapAnimationSpec = tween(),
        decayAnimationSpec = exponentialDecay()
    )

    /**
     * 安全读取 Offset 避免抛出未初始化异常或返回 NaN
     */
    val safeOffset: Float
        get() = try {
            val offset = anchoredDraggableState.offset
            if (offset.isNaN()) 0f else offset
        } catch (e: IllegalStateException) {
            0f // 处理未初始化的情况
        }
}

suspend fun RevealState.resetAnimated() {
    anchoredDraggableState.animateTo(targetValue = RevealValue.Default)
}

suspend fun RevealState.reset() {
    anchoredDraggableState.snapTo(targetValue = RevealValue.Default)
}