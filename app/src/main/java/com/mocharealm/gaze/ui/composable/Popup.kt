package com.mocharealm.gaze.ui.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.Backdrop
import com.mocharealm.gaze.glassy.liquid.effect.BackdropEffectScope
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.rememberListPopupLayoutInfo
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

object OverlayPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: PopupPositionProvider.Align
    ): IntOffset {
        val x = when (alignment) {
            PopupPositionProvider.Align.Start,
            PopupPositionProvider.Align.TopStart,
            PopupPositionProvider.Align.BottomStart -> {
                if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left + popupMargin.left
                else anchorBounds.right - popupMargin.right
            }

            PopupPositionProvider.Align.End,
            PopupPositionProvider.Align.TopEnd,
            PopupPositionProvider.Align.BottomEnd -> {
                if (layoutDirection == LayoutDirection.Ltr) anchorBounds.right - popupMargin.right
                else anchorBounds.left + popupMargin.left
            }
        }
        val y = when (alignment) {
            PopupPositionProvider.Align.TopStart,
            PopupPositionProvider.Align.TopEnd -> {
                anchorBounds.top + popupMargin.top
            }

            PopupPositionProvider.Align.BottomStart,
            PopupPositionProvider.Align.BottomEnd -> {
                anchorBounds.bottom - popupContentSize.height - popupMargin.bottom
            }

            else -> {
                anchorBounds.top
            }
        }

        return IntOffset(x, y)
    }

    override fun getMargins(): PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
}

@Composable
fun PopupMenu(
    show: MutableState<Boolean>,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    effects: BackdropEffectScope.() -> Unit = {
        vibrancy()
        blur(2f.dp.toPx())
        lens(12f.dp.toPx(), 24f.dp.toPx())
    },
    shadow: (() -> Shadow?)? = { Shadow.Default },
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val animationProgress = remember { Animatable(0f) }
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val coroutineScope = rememberCoroutineScope()
    val internalPopupState = remember { mutableStateOf(show.value) }
    val layoutDirection = LocalLayoutDirection.current

    // 动画逻辑
    LaunchedEffect(show.value) {
        if (show.value) {
            internalPopupState.value = true
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 362.5f,
                    visibilityThreshold = 0.0001f
                ),
            )
        } else {
            animationProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(300, easing = DecelerateEasing(1.5f)),
            )
            internalPopupState.value = false
        }
    }

    // 侧滑返回处理
    val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigationEventState,
        isBackEnabled = show.value,
        onBackCancelled = {
            coroutineScope.launch {
                animationProgress.animateTo(
                    1f,
                    animationSpec = tween(300, easing = DecelerateEasing(1.5f))
                )
            }
        },
        onBackCompleted = {
            currentOnDismiss?.invoke()
        },
    )

    LaunchedEffect(navigationEventState.transitionState) {
        val transitionState = navigationEventState.transitionState
        if (transitionState is NavigationEventTransitionState.InProgress &&
            transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
        ) {
            val progress = transitionState.latestEvent.progress
            animationProgress.snapTo(1f - progress)
        }
    }

    if (!show.value && !internalPopupState.value) return

    var parentBounds by remember { mutableStateOf(IntRect.Zero) }
    Spacer(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            // 注意：PopupMenu 应该放在触发它的组件同级或内部
            coordinates.parentLayoutCoordinates?.let { parentLayout ->
                val positionInWindow = parentLayout.positionInWindow()
                parentBounds = IntRect(
                    left = positionInWindow.x.toInt(),
                    top = positionInWindow.y.toInt(),
                    right = (positionInWindow.x + parentLayout.size.width).toInt(),
                    bottom = (positionInWindow.y + parentLayout.size.height).toInt(),
                )
            }
        },
    )

    if (parentBounds == IntRect.Zero) return

    var popupContentSize by remember { mutableStateOf(IntSize.Zero) }
    val layoutInfo = rememberListPopupLayoutInfo(
        alignment = alignment,
        popupPositionProvider = popupPositionProvider,
        parentBounds = parentBounds,
        popupContentSize = popupContentSize,
    )

    PopupLayout(
        visible = internalPopupState,
        enterTransition = fadeIn(),
        exitTransition = fadeOut(),
        enableWindowDim = false,
        enableBackHandler = false
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { currentOnDismiss?.invoke() })
                },
        ) {
            LiquidSurface(
                backdrop = backdrop,
                modifier = modifier
                    .onGloballyPositioned {
                        if (popupContentSize != it.size) {
                            popupContentSize = it.size
                        }
                    }
                    .layout { measurable, constraints ->
                        val maxH =
                            (layoutInfo.windowBounds.height - layoutInfo.popupMargin.top - layoutInfo.popupMargin.bottom)
                                .coerceAtLeast(50.dp.roundToPx())

                        val placeable = measurable.measure(
                            constraints.copy(
                                maxHeight = maxH.coerceAtMost(constraints.maxHeight),
                                minHeight = 0
                            )
                        )

                        // 实时计算当前测量大小下的位置偏移
                        val calculatedOffset = popupPositionProvider.calculatePosition(
                            parentBounds,
                            layoutInfo.windowBounds,
                            layoutDirection,
                            IntSize(placeable.width, placeable.height),
                            layoutInfo.popupMargin,
                            alignment,
                        )

                        layout(constraints.maxWidth, constraints.maxHeight) {
                            // 使用全屏坐标放置
                            placeable.place(calculatedOffset)
                        }
                    }
                    .pointerInput(Unit) {
                        // 防止点击菜单内容触发背景的 dismiss
                        detectTapGestures(onTap = { /* 消费点击事件 */ })
                    },
                tint = tint,
                surfaceColor = surfaceColor,
                shape = { ContinuousRoundedRectangle(24.dp) },
                effects = effects,
                shadow = shadow,
                content = content
            )
        }
    }
}