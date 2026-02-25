package com.mocharealm.compound.ui.composable.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.composable.base.SpoilerImage
import com.mocharealm.compound.ui.screen.chat.LocalOnDownloadVideo
import com.mocharealm.compound.ui.screen.chat.LocalVideoDownloadProgress
import com.mocharealm.compound.ui.util.SpoilerShader
import com.mocharealm.compound.ui.util.formatMessageTimestamp
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.zip.GZIPInputStream
import kotlin.math.hypot

@Composable
fun ReplyPreview(
    senderName: String,
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .clip(ContinuousRoundedRectangle(8.dp))
                .background(MiuixTheme.colorScheme.onSurfaceContainer.copy(0.1f))
                .clickable(onClick = onClick)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(accentColor, size = Size(4.dp.toPx(), size.height))
                    }
                },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .padding(start = 4.dp)
            .padding(8.dp)) {
            Text(
                text = senderName,
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = text,
                style = MiuixTheme.textStyles.footnote1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PhotoBlock(block: MessageBlock.MediaBlock) {
    if (!block.file.fileUrl.isNullOrEmpty()) {
        SpoilerImage(hasSpoiler = block.hasSpoiler, modifier = Modifier.wrapContentWidth()) {
            AsyncImage(
                model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(block.file.fileUrl)
                        .build(),
                contentDescription = "Photo",
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .wrapContentWidth(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun VideoBlock(block: MessageBlock.MediaBlock) {
    val maxWidth = 280.dp
    val aspectRatio =
        if (block.width > 0 && block.height > 0) block.width.toFloat() / block.height.toFloat()
        else 16f / 9f
    val videoModifier = Modifier
        .width(maxWidth)
        .height(maxWidth / aspectRatio)

    SpoilerImage(hasSpoiler = block.hasSpoiler, modifier = Modifier.wrapContentSize()) {
        if (!block.file.fileUrl.isNullOrEmpty()) {
            MessageVideoPlayer(filePath = block.file.fileUrl, modifier = videoModifier)
        } else {
            VideoThumbnailOverlay(block = block, modifier = videoModifier)
        }
    }
}

@Composable
fun StickerBlock(block: MessageBlock.StickerBlock, modifier: Modifier = Modifier) {
    if (!block.file.fileUrl.isNullOrEmpty()) {
        AsyncImage(
            model =
                ImageRequest.Builder(LocalContext.current)
                    .data(java.io.File(block.file.fileUrl))
                    .build(),
            contentDescription = "Sticker",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DocumentBlock(block: MessageBlock.DocumentBlock, modifier: Modifier = Modifier) {
    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(SFIcons.Document_Fill, null, Modifier.size(32.dp), MiuixTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = block.document.fileName,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = block.document.mimeType,
                style = MiuixTheme.textStyles.footnote1,
            )
        }
    }
}

@Composable
fun RichTextContent(
    text: androidx.compose.ui.text.AnnotatedString,
    contentColor: Color,
    modifier: Modifier,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    revealedEntityIndices: Set<Int>,
    onSpoilerClick: (Int) -> Unit
) {
    if (text.spanStyles.isEmpty() && text.getStringAnnotations(0, text.length).isEmpty()) {
        Text(text = text.text, color = contentColor, modifier = modifier)
        return
    }

    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }
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

    val shader = remember { SpoilerShader.getShader() }
    val brush = remember(shader) { ShaderBrush(shader) }
    val revealingSpoilers = remember {
        mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>()
    }
    val revealingOrigins = remember { mutableStateMapOf<Int, Offset>() }
    val coroutineScope = rememberCoroutineScope()

    val spoilerPaths =
        remember(text, layoutResult.value) {
            val layout = layoutResult.value ?: return@remember emptyMap<Int, Path>()
            val paths = mutableMapOf<Int, Path>()
            text.getStringAnnotations("SPOILER", 0, text.length).forEach { range ->
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
            onTextLayout = { layoutResult.value = it },
            modifier =
                modifier
                    .drawWithContent {
                        val obscuredPath = Path()
                        var hasObscured = false
                        spoilerPaths.forEach { (index, path) ->
                            if (index !in revealedEntityIndices ||
                                index in revealingSpoilers
                            ) {
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
                                canvas.saveLayer(
                                    Rect(0f, 0f, size.width, size.height),
                                    Paint()
                                )

                                revealingSpoilers.forEach { (index, anim) ->
                                    revealingOrigins[index]?.let { origin ->
                                        val radius = anim.value
                                        if (radius > 0f) {
                                            drawCircle(
                                                brush =
                                                    Brush.radialGradient(
                                                        0.0f to Color.Black,
                                                        0.5f to Color.Black,
                                                        1.0f to
                                                                Color.Transparent,
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
                            canvas.saveLayer(
                                Rect(0f, 0f, size.width, size.height),
                                Paint()
                            )

                            shader.setFloatUniform(
                                "particleColor",
                                contentColor.red,
                                contentColor.green,
                                contentColor.blue,
                                contentColor.alpha
                            )
                            shader.setFloatUniform("time", time)
                            shader.setFloatUniform(
                                "resolution",
                                size.width,
                                size.height
                            )
                            drawRect(brush = brush)

                            if (hasRipples) {
                                revealingSpoilers.forEach { (index, anim) ->
                                    revealingOrigins[index]?.let { origin ->
                                        val radius = anim.value
                                        if (radius > 0f) {
                                            drawCircle(
                                                brush =
                                                    Brush.radialGradient(
                                                        0.0f to Color.Black,
                                                        0.5f to Color.Black,
                                                        1.0f to
                                                                Color.Transparent,
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
                            if (pos.y < 0 || pos.y > layout.size.height)
                                return@detectTapGestures
                            val offset = layout.getOffsetForPosition(pos)

                            text.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()
                                ?.let { uriHandler.openUri(it.item) }

                            text.getStringAnnotations("SPOILER", offset, offset)
                                .firstOrNull()
                                ?.let {
                                    val index = it.item.toIntOrNull() ?: return@let
                                    if (index in revealedEntityIndices ||
                                        revealingSpoilers.containsKey(
                                            index
                                        )
                                    )
                                        return@let

                                    val maxRadius =
                                        hypot(
                                            layout.size.width
                                                .toDouble(),
                                            layout.size.height
                                                .toDouble()
                                        )
                                            .toFloat()
                                    val anim = Animatable(0f)
                                    revealingSpoilers[index] = anim
                                    revealingOrigins[index] = pos

                                    coroutineScope.launch {
                                        anim.animateTo(
                                            targetValue = maxRadius,
                                            animationSpec =
                                                tween(
                                                    durationMillis =
                                                        400,
                                                    easing =
                                                        LinearEasing
                                                )
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

@Composable
fun LottieSticker(filePath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val jsonString by
    produceState<String?>(initialValue = null, filePath) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    val uri = filePath.toUri()
                    val inputStream =
                        if (filePath.startsWith("file:///android_asset/")) {
                            val assetPath =
                                filePath.removePrefix("file:///android_asset/")
                            context.assets.open(assetPath)
                        } else {
                            context.contentResolver.openInputStream(uri)
                        }

                    inputStream?.use { input ->
                        GZIPInputStream(input).bufferedReader().use { it.readText() }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
    }

    if (jsonString != null) {
        val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(jsonString!!))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            modifier = modifier
        )
    }
}

@Composable
fun VideoThumbnailOverlay(block: MessageBlock.MediaBlock, modifier: Modifier = Modifier) {
    val downloadPercent = LocalVideoDownloadProgress.current[block.id]
    val onDownloadVideo = LocalOnDownloadVideo.current
    val layerBackdrop = rememberLayerBackdrop {
        drawRect(Color.Black)
        drawContent()
    }

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHighest)
                .clickable { if (downloadPercent == null) onDownloadVideo(block.id) },
        contentAlignment = Alignment.Center
    ) {
        if (!block.thumbnail?.fileUrl.isNullOrEmpty()) {
            AsyncImage(
                model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(java.io.File(block.thumbnail!!.fileUrl!!))
                        .build(),
                contentDescription = "Video thumbnail",
                modifier = Modifier
                    .matchParentSize()
                    .layerBackdrop(layerBackdrop),
                contentScale = ContentScale.Crop
            )
        }

        LiquidSurface(
            layerBackdrop,
            if (downloadPercent != null) Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            else Modifier.size(48.dp),
            Modifier.clickable { onDownloadVideo(block.id) },
            effects = {
                vibrancy()
                if (downloadPercent != null) blur(1.dp.toPx())
                lens(if (downloadPercent != null) 32.dp.toPx() else 16.dp.toPx(), 48.dp.toPx())
            },
            surfaceColor = MiuixTheme.colorScheme.surface.copy(alpha = 0.6f)
        ) {
            if (downloadPercent != null) {
                Text(
                    text = "$downloadPercent%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                Icon(
                    SFIcons.Play_Fill,
                    null,
                    Modifier.align(Alignment.Center),
                    MiuixTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TimestampLabel(timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = timestamp.formatMessageTimestamp(),
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            textAlign = TextAlign.Center,
            style = MiuixTheme.textStyles.footnote1
        )
    }
}

@Composable
fun SystemMessage(block: MessageBlock.SystemActionBlock) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val text =
            when (val type = block.type) {
                is MessageBlock.SystemActionBlock.SystemActionType.MemberJoined ->
                    tdString(
                        "NotificationGroupAddMember",
                        "un1" to type.actorName,
                        "un2" to type.targetName
                    )

                is MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink ->
                    tdString("ActionInviteUser", "un1" to type.userName)

//                is MessageBlock.SystemActionBlock.SystemActionType.MemberLeft ->
//                    tdString("EventLogLeftGroup", "un1" to type.userName)

                is MessageBlock.SystemActionBlock.SystemActionType.ChatChangedTitle ->
                    tdString(
                        "ActionChatEditTitle",
                        "un1" to type.actorName,
                        "title" to type.newTitle
                    )

//                is MessageBlock.SystemActionBlock.SystemActionType.ChatChangedPhoto ->
//                    tdString("ActionChatEditPhoto", "un1" to type.userName)
//
//                is MessageBlock.SystemActionBlock.SystemActionType.ChatUpgradedTo ->
//                    tdString(
//                        "ActionChatUpgradeTo",
//                        "supergroupId" to type.supergroupId.toString()
//                    )

                is MessageBlock.SystemActionBlock.SystemActionType.PinMessage ->
                    tdString("ActionPinnedText", "un1" to type.actorName)

//                is MessageBlock.SystemActionBlock.SystemActionType.Unknown ->
//                    "Unsupported system message"
            }

        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            textAlign = TextAlign.Center,
            style = MiuixTheme.textStyles.footnote1
        )
    }
}
