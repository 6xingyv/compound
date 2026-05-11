package com.mocharealm.compound.ui.screen.chat.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.composable.base.Avatar
import com.mocharealm.compound.ui.composable.chat.RichText
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.screen.chat.ChatUiState
import com.mocharealm.compound.ui.screen.chat.ChatViewModel
import com.mocharealm.compound.ui.util.PaddingValuesSide
import com.mocharealm.compound.ui.util.takeOnly
import com.mocharealm.compound.ui.util.toAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.Backdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import com.mocharealm.gaze.icons.Chevron_Backward
import com.mocharealm.gaze.icons.Chevron_Compact_Forward
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.icons.Video_Fill
import com.mocharealm.gaze.nav.LocalBackButtonVisibility
import com.mocharealm.gaze.ui.animation.InteractiveHighlight
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.tci18n.core.tdString
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun ChatTopBar(
    state: ChatUiState,
    viewModel: ChatViewModel,
    layerBackdrop: Backdrop
) {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val surfaceContainerColor = MiuixTheme.colorScheme.surfaceContainer
    val primaryColor = MiuixTheme.colorScheme.primary
    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val containerWidth = LocalWindowInfo.current.containerDpSize.width
    val navigator = LocalNavigator.current
    val captionBar = WindowInsets.captionBar.asPaddingValues()

    Column(
        Modifier
            .fillMaxWidth()
            .drawWithCache {
                onDrawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to surfaceColor.copy(1f),
                            1f to surfaceColor.copy(0f),
                            startY = statusBarHeightPx / 2f
                        ),
                    )
                }
            }
            .statusBarsPadding()
            .padding(captionBar.takeOnly(PaddingValuesSide.Top)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    AnimatedVisibility(LocalBackButtonVisibility.current) {
                        LiquidSurface(
                            layerBackdrop,
                            Modifier.size(48.dp),
                            Modifier.clickable { navigator.pop() },
                            effects = {
                                vibrancy()
                                blur(1.dp.toPx())
                                lens(
                                    16.dp.toPx(),
                                    32.dp.toPx(),
                                    chromaticAberration = false
                                )
                            },
                            shadow = {
                                Shadow(
                                    radius = 24f.dp,
                                    offset = DpOffset(0.dp, 0.dp),
                                    color = Color.Black.copy(alpha = 0.1f),
                                    alpha = 1f,
                                    blendMode = DrawScope.DefaultBlendMode
                                )
                            },
                        ) {
                            Icon(
                                SFIcons.Chevron_Backward,
                                null,
                                Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Row {
                    LiquidSurface(
                        layerBackdrop,
                        Modifier.size(48.dp),
                        effects = {
                            vibrancy()
                            blur(1.dp.toPx())
                            lens(16.dp.toPx(), 32.dp.toPx(), chromaticAberration = false)
                        },
                        shadow = {
                            Shadow(
                                radius = 24f.dp,
                                offset = DpOffset(0.dp, 0.dp),
                                color = Color.Black.copy(alpha = 0.1f),
                                alpha = 1f,
                                blendMode = DrawScope.DefaultBlendMode
                            )
                        },
                    ) { Icon(SFIcons.Video_Fill, null, Modifier.align(Alignment.Center)) }
                }
            }
            state.chatInfo?.let { chatInfo ->
                val animationScope = rememberCoroutineScope()

                val interactiveHighlight = remember(animationScope) {
                    InteractiveHighlight(
                        animationScope = animationScope
                    )
                }
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val width = size.width
                            val height = size.height

                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX =
                                maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY =
                                maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(
                                    1f
                                )
                            scaleY =
                                scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(
                                    1f
                                )
                        }
                        .then(interactiveHighlight.gestureModifier),
                    verticalArrangement = Arrangement.spacedBy((-6).dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Avatar(
                        initials = chatInfo.title.content.take(2),
                        modifier = Modifier
                            .size(48.dp)
                            .zIndex(20f)
                            .dropShadow(CircleShape) {
                                radius = 24f.dp.toPx()
                                offset = Offset(0f, radius / 6f)
                                color = Color.Black.copy(alpha = 0.1f)
                            },
                        photoPath = chatInfo.photoUrl
                    )
                    LiquidSurface(
                        layerBackdrop, Modifier.widthIn(
                            max = (containerWidth - 160.dp).coerceAtLeast(0.dp)
                        ), isInteractive = false
                    ) {
                        Row(
                            Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RichText(
                                text = chatInfo.title.toAnnotatedString(),
                                style = MiuixTheme.textStyles.footnote1.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                isInteractive = false
                            )
                            Icon(
                                SFIcons.Chevron_Compact_Forward,
                                null,
                                Modifier
                                    .width(16.dp)
                                    .graphicsLayer {
                                        blendMode =
                                            if (isDark) BlendMode.Plus else BlendMode.Multiply
                                        alpha = 0.6f
                                    })
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.pinnedMessages.isNotEmpty()) {
            state.pinnedMessages.getOrNull(state.currentPinnedIndex % state.pinnedMessages.size)
                ?.let { pinnedMessage ->
                    val pinnedText =
                        pinnedMessage.blocks.find { it is MessageBlock.TextBlock }
                            .let { if (it is MessageBlock.TextBlock) it.content.content else "Media" }
                    LiquidSurface(
                        backdrop = layerBackdrop,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        afterModifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Button,
                            onClick = {
                                viewModel.cyclePinnedMessages()
                            }
                        ),
                        shape = { ContinuousRoundedRectangle(16.dp) },
                        surfaceColor = surfaceContainerColor.copy(alpha = 0.6f),
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                        },
                    ) {
                        Row(
                            Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .width(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(primaryColor)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = tdString("PinnedMessage"),
                                    style = MiuixTheme.textStyles.footnote1,
                                    color = primaryColor
                                )
                                Text(
                                    text = pinnedText,
                                    style = MiuixTheme.textStyles.footnote1,
                                    modifier = Modifier.alpha(0.6f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
        }
    }
}
