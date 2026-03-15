package com.mocharealm.compound.ui.composable.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.airbnb.lottie.RenderMode
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.maplibre.compose.MapView
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.camera.MapViewCamera
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.composable.base.SpoilerImage
import com.mocharealm.compound.ui.composable.base.VideoPlayer
import com.mocharealm.compound.ui.composable.base.VpxVideoPlayer
import com.mocharealm.compound.ui.screen.chat.LocalDocumentDownloadProgress
import com.mocharealm.compound.ui.screen.chat.LocalOnDownloadDocument
import com.mocharealm.compound.ui.screen.chat.LocalOnDownloadVideo
import com.mocharealm.compound.ui.screen.chat.LocalOnMediaClick
import com.mocharealm.compound.ui.screen.chat.LocalVideoDownloadProgress
import com.mocharealm.compound.ui.util.LocalSharedTransitionScope
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
import kotlinx.coroutines.withContext
import org.maplibre.android.maps.MapLibreMapOptions
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.File
import java.util.zip.GZIPInputStream

@Composable
fun ReplyPreview(
    senderName: String,
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    Row(
        modifier =
            modifier
                .clip(ContinuousRoundedRectangle(8.dp))
                .background(MiuixTheme.colorScheme.onSurfaceContainer.copy(0.1f))
                .clickable(onClick = onClick)
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            accentColor,
                            topLeft = Offset(if (isLtr) 0f else size.width - 4.dp.toPx(), 0f),
                            size = Size(4.dp.toPx(), size.height)
                        )
                    }
                },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .padding(start = 4.dp)
                .padding(8.dp)
        ) {
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
fun PhotoBlock(
    block: MessageBlock.MediaBlock,
    modifier: Modifier = Modifier.wrapContentWidth(),
    imageModifier: Modifier = Modifier.fillMaxSize(),
    contentScale: ContentScale = ContentScale.Crop
) {
    val onMediaClick = LocalOnMediaClick.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current

    val maxWidth = 280.dp
    val aspectRatio =
        if (block.width > 0 && block.height > 0) block.width.toFloat() / block.height.toFloat()
        else 1f

    val finalContainerModifier = modifier
        .widthIn(max = maxWidth)
        .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
        .clickable { onMediaClick(block.id) }

    val sharedModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            rememberSharedContentState(key = "media_${block.id}"),
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    val displayUrl = block.file.fileUrl ?: block.thumbnail?.fileUrl

    SpoilerImage(hasSpoiler = block.hasSpoiler, modifier = finalContainerModifier) {
        if (!displayUrl.isNullOrEmpty()) {
            AsyncImage(
                model =
                ImageRequest.Builder(LocalContext.current)
                    .data(displayUrl)
                    .build(),
                contentDescription = "Photo",
                modifier = imageModifier.then(sharedModifier),
                contentScale = contentScale
            )
        } else {
            Box(imageModifier.then(sharedModifier))
        }
    }
}

@Composable
fun VideoBlock(
    block: MessageBlock.MediaBlock,
    modifier: Modifier = Modifier.wrapContentSize(),
    videoModifier: Modifier? = null
) {
    val onMediaClick = LocalOnMediaClick.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedContentScope.current

    val maxWidth = 280.dp
    val aspectRatio =
        if (block.width > 0 && block.height > 0) block.width.toFloat() / block.height.toFloat()
        else 16f / 9f

    val finalVideoModifier = videoModifier ?: Modifier.fillMaxSize()

    val finalContainerModifier = modifier
        .width(maxWidth)
        .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
        .clickable { onMediaClick(block.id) }

    val sharedModifier = with(sharedTransitionScope) {
        Modifier.sharedElement(
            rememberSharedContentState(key = "media_${block.id}"),
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    SpoilerImage(hasSpoiler = block.hasSpoiler, modifier = finalContainerModifier) {
        if (!block.file.fileUrl.isNullOrEmpty()) {
            MessageVideoPlayer(filePath = block.file.fileUrl, modifier = finalVideoModifier.then(sharedModifier))
        } else {
            VideoThumbnailOverlay(block = block, modifier = finalVideoModifier.then(sharedModifier))
        }
    }
}

@Composable
fun StickerBlock(
    block: MessageBlock.StickerBlock,
    modifier: Modifier = Modifier,
    useTextureView: Boolean = true,
) {
    if (!block.file.fileUrl.isNullOrEmpty()) {
        when (block.stickerFormat) {
            MessageBlock.StickerBlock.StickerFormat.WEBM -> VpxVideoPlayer(
                filePath = block.file.fileUrl,
                modifier = modifier
            )

            MessageBlock.StickerBlock.StickerFormat.MP4 -> VideoPlayer(
                filePath = block.file.fileUrl,
                modifier = modifier
            )

            MessageBlock.StickerBlock.StickerFormat.TGS -> LottieSticker(
                filePath = block.file.fileUrl,
                modifier = modifier,
                useTextureView = useTextureView
            )

            else -> AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(block.file.fileUrl)).build(),
                contentDescription = "Sticker",
                modifier = modifier,
                contentScale = ContentScale.Fit
            )
        }
    } else if (!block.thumbnail?.fileUrl.isNullOrEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(block.thumbnail.fileUrl)).build(),
            contentDescription = "Sticker",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun DocumentBlock(block: MessageBlock.DocumentBlock, modifier: Modifier = Modifier) {
    val onDownloadDocument = LocalOnDownloadDocument.current
    val downloadPercent = LocalDocumentDownloadProgress.current[block.id]
    val isDownloaded = !block.document.file.fileUrl.isNullOrEmpty()

    Row(
        modifier = modifier.clickable { onDownloadDocument(block.id) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (isDownloaded) SFIcons.Document_Fill else SFIcons.Arrow_Down_Document,
                null,
                Modifier.size(32.dp),
                LocalContentColor.current
            )
            if (downloadPercent != null) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$downloadPercent%",
                        style = MiuixTheme.textStyles.footnote1,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = block.document.fileName,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = when {
                    downloadPercent != null -> tdString("Loading")
                    isDownloaded -> tdString("MessageOpen")
                    else -> block.document.mimeType
                },
                style = MiuixTheme.textStyles.footnote1,
            )
        }
    }
}

@Composable
fun VenueBlock(block: MessageBlock.VenueBlock, modifier: Modifier = Modifier) {
    val camera = rememberSaveableMapViewCamera(
        MapViewCamera(
            CameraState.Centered(
                block.venue.latitude,
                block.venue.longitude,
            ),
        )
    )
    val mapOptions = remember {
        MapLibreMapOptions().apply {
            scrollGesturesEnabled(false)
            zoomGesturesEnabled(false)
            tiltGesturesEnabled(false)
            rotateGesturesEnabled(false)
            doubleTapGesturesEnabled(false)
            textureMode(true)
        }
    }
    MapView(
        modifier = modifier.aspectRatio(1.2f),
        camera = camera,
        styleUrl = "https://tiles.openfreemap.org/styles/bright",
        mapOptions = mapOptions
    ) {
    }
}


@Composable
fun LottieSticker(
    filePath: String,
    modifier: Modifier = Modifier,
    useTextureView: Boolean = true,
) {
    val context = LocalContext.current

    val jsonString by
    produceState<String?>(initialValue = null, filePath) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    val file = File(filePath)
                    val inputStream =
                        if (filePath.startsWith("file:///android_asset/")) {
                            val assetPath =
                                filePath.removePrefix("file:///android_asset/")
                            context.assets.open(assetPath)
                        } else if (file.exists()) {
                            file.inputStream()
                        } else {
                            val uri = filePath.toUri()
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
            modifier = modifier,
            renderMode = if (useTextureView) RenderMode.AUTOMATIC else RenderMode.HARDWARE
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
                .clickable { if (downloadPercent == null) onDownloadVideo(block.id) },
        contentAlignment = Alignment.Center
    ) {
        if (!block.thumbnail?.fileUrl.isNullOrEmpty()) {
            AsyncImage(
                model =
                    ImageRequest.Builder(LocalContext.current)
                        .data(File(block.thumbnail.fileUrl))
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
