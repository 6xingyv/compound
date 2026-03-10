package com.mocharealm.compound.ui.composable.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.compound.ui.screen.chat.LocalCustomEmojiStickers
import com.mocharealm.compound.ui.util.RichTextTags
import com.mocharealm.compound.ui.util.SpoilerShader
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.hypot

@Composable
fun RichText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalContentColor.current,
    style: TextStyle = MiuixTheme.textStyles.body1,
    revealedEntityIndices: Set<Int> = emptySet(),
    onSpoilerClick: (Int) -> Unit = {},
    onMentionClick: (String) -> Unit = {}
) {
    val customEmojiStickers = LocalCustomEmojiStickers.current
    val uriHandler = LocalUriHandler.current

    val inlineContent = remember(text, customEmojiStickers) {
        text.getStringAnnotations(RichTextTags.CUSTOM_EMOJI, 0, text.length).associate { range ->
            val emojiId = range.item.toLongOrNull() ?: return@associate range.item to InlineTextContent(Placeholder(0.sp, 0.sp, PlaceholderVerticalAlign.Center)) {}
            val sticker = customEmojiStickers[emojiId]
            range.item to InlineTextContent(
                Placeholder(22.sp, 22.sp, PlaceholderVerticalAlign.Center)
            ) {
                if (sticker != null) {
                    StickerBlock(
                        block = sticker,
                        modifier = Modifier.size(22.dp),
                        useTextureView = false
                    )
                } else {
                    Box(
                        Modifier
                            .size(22.dp)
                            .background(Color.Gray.copy(0.1f))
                    )
                }
            }
        }
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
    var time by remember { mutableFloatStateOf(0f) }

    val hasSpoilers = remember(text) {
        text.getStringAnnotations(RichTextTags.SPOILER, 0, text.length).isNotEmpty()
    }

    LaunchedEffect(hasSpoilers) {
        if (!hasSpoilers) return@LaunchedEffect
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

    val shader = remember { SpoilerShader.getShader() }
    val brush = remember(shader) { ShaderBrush(shader) }
    val revealingSpoilers = remember {
        mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
    }
    val revealingOrigins = remember { mutableStateMapOf<Int, Offset>() }
    val coroutineScope = rememberCoroutineScope()

    val spoilerPaths = remember(text, layoutResult.value) {
        val layout = layoutResult.value ?: return@remember emptyMap<Int, Path>()
        val paths = mutableMapOf<Int, Path>()
        text.getStringAnnotations(RichTextTags.SPOILER, 0, text.length).forEach { range ->
            range.item.toIntOrNull()?.let { index ->
                paths[index] = layout.multiParagraph.getPathForRange(range.start, range.end)
            }
        }
        paths
    }

    SelectionContainer {
        Text(
            text = text,
            color = contentColor,
            style = style,
            onTextLayout = { layoutResult.value = it },
            inlineContent = inlineContent,
            modifier = modifier
                .drawWithContent {
                    val obscuredPath = Path()
                    var hasObscured = false
                    spoilerPaths.forEach { (index, path) ->
                        if (index !in revealedEntityIndices || index in revealingSpoilers) {
                            obscuredPath.addPath(path)
                            hasObscured = true
                        }
                    }

                    if (!hasObscured) {
                        drawContent()
                        return@drawWithContent
                    }

                    clipPath(obscuredPath, clipOp = ClipOp.Difference) {
                        this@drawWithContent.drawContent()
                    }

                    val hasRipples = revealingSpoilers.isNotEmpty()
                    if (hasRipples) {
                        clipPath(obscuredPath) {
                            val canvas = drawContext.canvas
                            canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())

                            revealingSpoilers.forEach { (index, anim) ->
                                revealingOrigins[index]?.let { origin ->
                                    val radius = anim.value
                                    if (radius > 0f) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                0.0f to Color.Black,
                                                0.5f to Color.Black,
                                                1.0f to Color.Transparent,
                                                center = origin,
                                                radius = radius
                                            ),
                                            center = origin,
                                            radius = radius
                                        )
                                    }
                                }
                            }

                            canvas.saveLayer(
                                Rect(0f, 0f, size.width, size.height),
                                Paint().apply { blendMode = BlendMode.SrcIn }
                            )
                            this@drawWithContent.drawContent()
                            canvas.restore()
                            canvas.restore()
                        }
                    }

                    clipPath(obscuredPath) {
                        val canvas = drawContext.canvas
                        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())

                        shader.setFloatUniform(
                            "particleColor",
                            contentColor.red,
                            contentColor.green,
                            contentColor.blue,
                            contentColor.alpha
                        )
                        shader.setFloatUniform("time", time)
                        shader.setFloatUniform("resolution", size.width, size.height)
                        drawRect(brush = brush)

                        if (hasRipples) {
                            revealingSpoilers.forEach { (index, anim) ->
                                revealingOrigins[index]?.let { origin ->
                                    val radius = anim.value
                                    if (radius > 0f) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                0.0f to Color.Black,
                                                0.5f to Color.Black,
                                                1.0f to Color.Transparent,
                                                center = origin,
                                                radius = radius
                                            ),
                                            center = origin,
                                            radius = radius,
                                            blendMode = BlendMode.DstOut
                                        )
                                    }
                                }
                            }
                        }
                        canvas.restore()
                    }
                }
                .pointerInput(text, layoutResult, revealedEntityIndices) {
                    detectTapGestures { pos ->
                        val layout = layoutResult.value ?: return@detectTapGestures
                        if (pos.y < 0 || pos.y > layout.size.height) return@detectTapGestures
                        val offset = layout.getOffsetForPosition(pos)

                        text.getStringAnnotations(RichTextTags.URL, offset, offset)
                            .firstOrNull()
                            ?.let { uriHandler.openUri(it.item) }

                        text.getStringAnnotations(RichTextTags.MENTION, offset, offset)
                            .firstOrNull()
                            ?.let { onMentionClick(it.item) }

                        text.getStringAnnotations(RichTextTags.SPOILER, offset, offset)
                            .firstOrNull()
                            ?.let {
                                val index = it.item.toIntOrNull() ?: return@let
                                if (index in revealedEntityIndices || revealingSpoilers.containsKey(index)) return@let

                                val maxRadius = hypot(layout.size.width.toDouble(), layout.size.height.toDouble()).toFloat()
                                val anim = Animatable(0f)
                                revealingSpoilers[index] = anim
                                revealingOrigins[index] = pos

                                coroutineScope.launch {
                                    anim.animateTo(
                                        targetValue = maxRadius,
                                        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
                                    )
                                    onSpoilerClick(index)
                                    yield()
                                    revealingSpoilers.remove(index)
                                    revealingOrigins.remove(index)
                                }
                            }
                    }
                }
        )
    }
}
