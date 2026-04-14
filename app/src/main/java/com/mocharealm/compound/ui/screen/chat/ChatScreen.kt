package com.mocharealm.compound.ui.screen.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maplibre.compose.MapView
import com.maplibre.compose.camera.CameraState
import com.maplibre.compose.rememberSaveableMapViewCamera
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.ui.composable.base.Avatar
import com.mocharealm.compound.ui.composable.chat.DocumentBlock
import com.mocharealm.compound.ui.composable.chat.MessageBubble
import com.mocharealm.compound.ui.composable.chat.PhotoBlock
import com.mocharealm.compound.ui.composable.chat.ReplyPreview
import com.mocharealm.compound.ui.composable.chat.RichText
import com.mocharealm.compound.ui.composable.chat.StickerBlock
import com.mocharealm.compound.ui.composable.chat.SystemMessage
import com.mocharealm.compound.ui.composable.chat.TimestampLabel
import com.mocharealm.compound.ui.composable.chat.VideoBlock
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.util.EmptyIndication
import com.mocharealm.compound.ui.util.MarkdownTransformation
import com.mocharealm.compound.ui.util.PaddingValuesSide
import com.mocharealm.compound.ui.util.formatName
import com.mocharealm.compound.ui.util.takeOnly
import com.mocharealm.compound.ui.util.toAnnotatedString
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import com.mocharealm.gaze.icons.Arrowshape_Turn_Up_Left_Fill
import com.mocharealm.gaze.icons.Chevron_Backward
import com.mocharealm.gaze.icons.Chevron_Compact_Forward
import com.mocharealm.gaze.icons.Document
import com.mocharealm.gaze.icons.Face_Smiling
import com.mocharealm.gaze.icons.Location_Fill
import com.mocharealm.gaze.icons.Mappin_And_Ellipse
import com.mocharealm.gaze.icons.Microphone
import com.mocharealm.gaze.icons.Paperplane
import com.mocharealm.gaze.icons.Photo_On_Rectangle_Angled
import com.mocharealm.gaze.icons.Plus
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.icons.Video_Fill
import com.mocharealm.gaze.icons.Xmark
import com.mocharealm.gaze.nav.LocalBackButtonVisibility
import com.mocharealm.gaze.ui.animation.InteractiveHighlight
import com.mocharealm.gaze.ui.composable.Button
import com.mocharealm.gaze.ui.composable.ElasticRevealSwipe
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.gaze.ui.composable.OverlayPositionProvider
import com.mocharealm.gaze.ui.composable.PopupMenu
import com.mocharealm.gaze.ui.composable.RevealDirection
import com.mocharealm.gaze.ui.composable.TextField
import com.mocharealm.gaze.ui.composable.rememberElasticRevealState
import com.mocharealm.gaze.ui.layout.imeNestedScroll
import com.mocharealm.gaze.ui.layout.imePadding
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.koin.androidx.compose.koinViewModel
import org.maplibre.android.maps.MapLibreMapOptions
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.io.File
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

val LocalVideoDownloadProgress = staticCompositionLocalOf<Map<Long, Int>> { emptyMap() }
val LocalOnDownloadVideo = staticCompositionLocalOf<(Long) -> Unit> { {} }
val LocalDocumentDownloadProgress = staticCompositionLocalOf<Map<Long, Int>> { emptyMap() }
val LocalOnDownloadDocument = staticCompositionLocalOf<(Long) -> Unit> { {} }
val LocalCustomEmojiStickers =
    staticCompositionLocalOf<Map<Long, MessageBlock.StickerBlock>> { emptyMap() }
val LocalOnMediaClick = staticCompositionLocalOf<(Long) -> Unit> { {} }

@OptIn(ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val navigator = LocalNavigator.current
    val listState = rememberLazyListState()

    val isDark = isSystemInDarkTheme()

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 25
        }
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val surfaceContainerColor = MiuixTheme.colorScheme.surfaceContainer
    val primaryColor = MiuixTheme.colorScheme.primary

    val layerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val containerWidth = LocalWindowInfo.current.containerDpSize.width
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val menuOpened = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = state.stickerPanelVisible || state.locationPanelVisible) {
        if (state.stickerPanelVisible) viewModel.hideStickerPanel()
        if (state.locationPanelVisible) viewModel.hideLocationPanel()
    }

    LaunchedEffect(state.stickerPanelVisible, state.locationPanelVisible) {
        if (state.stickerPanelVisible || state.locationPanelVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        val files = uris.mapNotNull { uri -> uriToShareFileInfo(context, uri) }
        viewModel.onFilesSelected(files)
    }

    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val files = uris.mapNotNull { uri -> uriToShareFileInfo(context, uri) }
        viewModel.onFilesSelected(files)
    }

    val shouldLoadNewer by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisibleItem != null && firstVisibleItem.index <= 25
        }
    }



    LaunchedEffect(shouldLoadMore, state.loadingMore, state.loading) {
        if (shouldLoadMore && !state.loadingMore && !state.loading && state.hasMore) {
            viewModel.loadOlderMessages()
        }
    }

    LaunchedEffect(shouldLoadNewer, state.loadingNewer, state.loading) {
        if (shouldLoadNewer && !state.loadingNewer && !state.loading && state.hasMoreNewer) {
            viewModel.loadNewerMessages()
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.debounce(500L)
            .collect { visibleItems ->
                val firstMessageKey = visibleItems.firstOrNull { it.key is Long }?.key as? Long
                if (firstMessageKey != null) {
                    viewModel.saveReadPosition(firstMessageKey)
                }
            }
    }

    val captionBar = WindowInsets.captionBar.asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
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
                    state.pinnedMessages.firstOrNull()?.let { pinnedMessage ->
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
                                    viewModel.scrollToMessage(pinnedMessage.id)
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
        },
        bottomBar = {
            Column(
                Modifier
                    .then(
                        if (state.stickerPanelVisible || state.locationPanelVisible) Modifier
                        else Modifier.imePadding()
                    )
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to surfaceColor.copy(0f), 1f to surfaceColor.copy(1f)
                                )
                            )
                        }
                    }
                    .fillMaxWidth()) {
                Row(
                    Modifier
                        .then(
                            if (state.stickerPanelVisible || state.locationPanelVisible || WindowInsets.isImeVisible) Modifier
                            else Modifier
                                .navigationBarsPadding()
                                .padding(captionBar.takeOnly(PaddingValuesSide.Bottom))
                        )
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(Modifier.width(16.dp))
                    SharedTransitionLayout(
                        Modifier.size(48.dp)
                    ) {
                        AnimatedContent(menuOpened.value) { menuOpenedFinal ->
                            if (menuOpenedFinal) {
                                PopupMenu(
                                    true,
                                    layerBackdrop,
                                    Modifier.sharedBounds(
                                        rememberSharedContentState(key = "bounds"),
                                        animatedVisibilityScope = this@AnimatedContent,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                    ),
                                    popupPositionProvider = OverlayPositionProvider,
                                    alignment = PopupPositionProvider.Align.BottomStart,
                                    surfaceColor = surfaceContainerColor.copy(0.4f),
                                    onDismissRequest = { menuOpened.value = false },
                                    effects = {
                                        blur(8.dp.toPx())
                                        lens(
                                            16.dp.toPx(), 32.dp.toPx(), chromaticAberration = false
                                        )
                                    }
                                ) {
                                    Column(
                                        Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        data class MenuItem(
                                            val label: String,
                                            val icon: androidx.compose.ui.graphics.vector.ImageVector,
                                            val onClick: () -> Unit
                                        )

                                        val list = listOf(
                                            MenuItem(
                                                tdString("AttachSticker"), SFIcons.Face_Smiling
                                            ) {
                                                menuOpened.value = false
                                                viewModel.showStickerPanel()
                                            }, MenuItem(
                                                tdString("ChatGallery"),
                                                SFIcons.Photo_On_Rectangle_Angled
                                            ) {
                                                menuOpened.value = false
                                                galleryLauncher.launch(
                                                    PickVisualMediaRequest(
                                                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                                    )
                                                )
                                            }, MenuItem(
                                                tdString("ChatDocument"), SFIcons.Document
                                            ) {
                                                menuOpened.value = false
                                                documentLauncher.launch(arrayOf("*/*"))
                                            }, MenuItem(
                                                tdString("ChatLocation"), SFIcons.Mappin_And_Ellipse
                                            ) {
                                                menuOpened.value = false
                                                viewModel.showLocationPanel()
                                            }
                                        )
                                        CompositionLocalProvider(LocalIndication provides EmptyIndication) {
                                            list.forEach { item ->
                                                Row(
                                                    Modifier.clickable(onClick = item.onClick),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Icon(
                                                        item.icon,
                                                        null,
                                                        Modifier
                                                            .clip(CircleShape)
                                                            .background(
                                                                Brush.verticalGradient(
                                                                    0f to Color.Gray,
                                                                    1f to Color.DarkGray
                                                                )
                                                            )
                                                            .padding(8.dp)
                                                            .size(20.dp),
                                                        tint = Color.White
                                                    )
                                                    Text(item.label)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LiquidSurface(
                                    layerBackdrop,
                                    Modifier
                                        .size(48.dp)
                                        .sharedBounds(
                                            rememberSharedContentState(key = "bounds"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            enter = fadeIn(),
                                            exit = fadeOut(),
                                            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
                                        ),
                                    Modifier.clickable { menuOpened.value = true },
                                    effects = {
                                        vibrancy()
                                        blur(1.dp.toPx())
                                        lens(
                                            16.dp.toPx(), 32.dp.toPx(), chromaticAberration = false
                                        )
                                    },
                                    surfaceColor = surfaceContainerColor.copy(alpha = 0.6f)
                                ) { Icon(SFIcons.Plus, null, Modifier.align(Alignment.Center)) }
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    CompositionLocalProvider(LocalIndication provides EmptyIndication) {
                        LiquidSurface(
                            layerBackdrop,
                            Modifier.weight(1f),
                            shape = { ContinuousRoundedRectangle(24.dp) },
                            effects = {
                                vibrancy()
                                blur(2.dp.toPx())
                                lens(15.dp.toPx(), 30.dp.toPx(), chromaticAberration = false)
                            },
                            surfaceColor = surfaceContainerColor.copy(alpha = 0.2f),
                            shadow = {
                                Shadow(
                                    radius = 24f.dp,
                                    offset = DpOffset(0.dp, 0.dp),
                                    color = Color.Black.copy(alpha = 0.1f),
                                    alpha = 1f,
                                    blendMode = DrawScope.DefaultBlendMode
                                )
                            }) {
                            Row(
                                Modifier
                                    .padding(start = 12.dp)
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var inAudioMode by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 24.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = inAudioMode,
                                        enter = slideInHorizontally { it },
                                        exit = slideOutHorizontally { it }) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                SFIcons.Microphone,
                                                null,
                                                modifier = Modifier.clickable {})
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = tdString("AccDescrVoiceMessage"),
                                                Modifier
                                                    .weight(1f)
                                                    .graphicsLayer {
                                                        blendMode =
                                                            if (isDark) BlendMode.Plus else BlendMode.Multiply
                                                        alpha = 0.6f
                                                    },
                                                style = MiuixTheme.textStyles.body1,
                                                maxLines = 1,
                                                softWrap = false,
                                            )
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = !inAudioMode,
                                        enter = slideInHorizontally { -it },
                                        exit = slideOutHorizontally { -it }) {
                                        Column(
                                            Modifier
                                                .animateContentSize()
                                                .padding(end = 12.dp)
                                        ) {
                                            AnimatedVisibility(state.replyingToMessage != null) {
                                                state.replyingToMessage?.let { replyTo ->
                                                    val replyText =
                                                        replyTo.blocks.find { b -> b is MessageBlock.TextBlock }
                                                            .let { b ->
                                                                if (b is MessageBlock.TextBlock) b.content.content else "Media"
                                                            }
                                                    Row(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(bottom = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        ReplyPreview(
                                                            senderName = replyTo.sender.formatName(),
                                                            text = replyText,
                                                            accentColor = primaryColor,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Icon(
                                                            SFIcons.Xmark,
                                                            null,
                                                            Modifier
                                                                .size(24.dp)
                                                                .clickable {
                                                                    viewModel.setReplyingTo(
                                                                        null
                                                                    )
                                                                }
                                                        )
                                                    }
                                                }
                                            }
                                            AnimatedVisibility(
                                                visible = state.selectedFiles.isNotEmpty(),
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                LazyRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        8.dp
                                                    )
                                                ) {
                                                    items(
                                                        state.selectedFiles,
                                                        key = { it.filePath }) { file ->
                                                        Box(
                                                            modifier = Modifier
                                                                .height(80.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                        ) {
                                                            val messageBlock = remember(file) {
                                                                if (file.mimeType.startsWith("image/")) {
                                                                    MessageBlock.MediaBlock(
                                                                        id = 0,
                                                                        timestamp = 0,
                                                                        mediaType = MessageBlock.MediaBlock.MediaType.PHOTO,
                                                                        file = com.mocharealm.compound.domain.model.File(
                                                                            fileUrl = file.filePath
                                                                        )
                                                                    )
                                                                } else if (file.mimeType.startsWith(
                                                                        "video/"
                                                                    )
                                                                ) {
                                                                    MessageBlock.MediaBlock(
                                                                        id = 0,
                                                                        timestamp = 0,
                                                                        mediaType = MessageBlock.MediaBlock.MediaType.VIDEO,
                                                                        file = com.mocharealm.compound.domain.model.File(
                                                                            fileUrl = file.filePath
                                                                        ),
                                                                        thumbnail = com.mocharealm.compound.domain.model.File(
                                                                            fileUrl = file.thumbnailPath
                                                                        )
                                                                    )
                                                                } else {
                                                                    val fileName =
                                                                        file.filePath.substringAfterLast(
                                                                            "/"
                                                                        )
                                                                    MessageBlock.DocumentBlock(
                                                                        id = 0,
                                                                        timestamp = 0,
                                                                        document = com.mocharealm.compound.domain.model.Document(
                                                                            file = com.mocharealm.compound.domain.model.File(
                                                                                fileUrl = file.filePath
                                                                            ),
                                                                            fileName = fileName,
                                                                            mimeType = file.mimeType
                                                                        )
                                                                    )
                                                                }
                                                            }

                                                            when (messageBlock) {
                                                                is MessageBlock.MediaBlock -> {
                                                                    if (messageBlock.mediaType == MessageBlock.MediaBlock.MediaType.PHOTO) {
                                                                        PhotoBlock(
                                                                            block = messageBlock,
                                                                            modifier = Modifier
                                                                                .fillMaxHeight()
                                                                                .aspectRatio(1f),
                                                                            imageModifier = Modifier.fillMaxSize(),
                                                                            contentScale = ContentScale.Crop
                                                                        )
                                                                    } else {
                                                                        VideoBlock(
                                                                            block = messageBlock,
                                                                            modifier = Modifier
                                                                                .fillMaxHeight()
                                                                                .aspectRatio(1f),
                                                                            videoModifier = Modifier.fillMaxSize()
                                                                        )
                                                                    }
                                                                }

                                                                is MessageBlock.DocumentBlock -> {
                                                                    Box(
                                                                        Modifier
                                                                            .fillMaxHeight()
                                                                            .width(200.dp)
                                                                            .background(
                                                                                surfaceContainerColor
                                                                            )
                                                                            .padding(horizontal = 12.dp),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        DocumentBlock(
                                                                            messageBlock
                                                                        )
                                                                    }
                                                                }

                                                                else -> {}
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(4.dp)
                                                                    .size(20.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        Color.Black.copy(
                                                                            alpha = 0.5f
                                                                        )
                                                                    )
                                                                    .clickable {
                                                                        viewModel.removeSelectedFile(
                                                                            file
                                                                        )
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    SFIcons.Xmark,
                                                                    null,
                                                                    Modifier.size(12.dp),
                                                                    tint = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            TextField(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(focusRequester)
                                                    .onFocusChanged {
                                                        if (it.isFocused) {
                                                            if (state.stickerPanelVisible) viewModel.hideStickerPanel()
                                                            if (state.locationPanelVisible) viewModel.hideLocationPanel()
                                                        }
                                                    },
                                                state = viewModel.inputState,
                                                outputTransformation = MarkdownTransformation,
                                                lineLimits = TextFieldLineLimits.MultiLine(),
                                                padding = 0.dp,
                                                clipRadius = 0.dp,
                                                activeBackgroundColor = Color.Transparent,
                                                inactiveBackgroundColor = Color.Transparent,
                                                activeBorderSize = 0.dp,
                                                inactiveBorderSize = 0.dp,
                                                textStyle = MiuixTheme.textStyles.body1,
                                                decorator = { innerTextField ->
                                                    if (viewModel.inputState.text.isEmpty()) {
                                                        Box(contentAlignment = Alignment.CenterStart) {
                                                            innerTextField()
                                                            Text(
                                                                tdString(
                                                                    "TypeMessage"
                                                                ),
                                                                modifier = Modifier.graphicsLayer {
                                                                    blendMode =
                                                                        if (isDark) BlendMode.Plus else BlendMode.Multiply
                                                                    alpha = 0.6f
                                                                },
                                                                style = MiuixTheme.textStyles.body1,
                                                            )
                                                        }
                                                    } else innerTextField()
                                                })
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = (inAudioMode || (viewModel.inputState.text.lines().size <= 1 && state.selectedFiles.isEmpty() && state.replyingToMessage == null)) && !state.loading,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 12.dp)
                                            .clickable {
                                                inAudioMode = !inAudioMode
                                            }) {
                                        AnimatedContent(
                                            targetState = inAudioMode, transitionSpec = {
                                                (fadeIn() + scaleIn()).togetherWith(
                                                    fadeOut() + scaleOut()
                                                )
                                            }, label = "IconSwitch"
                                        ) { isAudio ->
                                            if (isAudio) {
                                                Icon(
                                                    SFIcons.Xmark,
                                                    contentDescription = "Close Audio"
                                                )
                                            } else {
                                                Icon(
                                                    SFIcons.Microphone,
                                                    contentDescription = "Open Audio"
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    AnimatedVisibility(
                        !state.loading && (viewModel.inputState.text.isNotBlank() || state.selectedFiles.isNotEmpty()),
                        Modifier,
                        enter = slideInHorizontally {
                            if (layoutDirection == LayoutDirection.Ltr) it
                            else -it
                        } + expandHorizontally(),
                        exit = slideOutHorizontally {
                            if (layoutDirection == LayoutDirection.Ltr) it
                            else -it
                        } + shrinkHorizontally()) {
                        LiquidSurface(
                            layerBackdrop,
                            Modifier
                                .padding(end = 16.dp)
                                .size(48.dp),
                            Modifier.clickable(onClick = viewModel::sendMessage),
                            effects = {
                                vibrancy()
                                blur(4.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx(), chromaticAberration = false)
                            },
                            tint = primaryColor,
                            shadow = {
                                Shadow(
                                    radius = 16.dp,
                                    offset = DpOffset(0.dp, 0.dp),
                                    alpha = 1f,
                                    blendMode = DrawScope.DefaultBlendMode
                                )
                            },
                        ) {
                            Icon(
                                SFIcons.Paperplane,
                                null,
                                Modifier.align(Alignment.Center),
                                Color.White
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = state.stickerPanelVisible || state.locationPanelVisible,
                    enter = slideInVertically { it } + expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = slideOutVertically { it } + shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
                    LiquidSurface(
                        layerBackdrop,
                        Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        isInteractive = false,
                        shape = {
                            ContinuousRoundedRectangle(
                                topStart = 24.dp, topEnd = 24.dp
                            )
                        },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            lens(12.dp.toPx(), 24.dp.toPx(), chromaticAberration = false)
                        },
                        shadow = {
                            Shadow(0.dp, DpOffset.Zero, Color.Transparent)
                        },
                        surfaceColor = surfaceContainerColor.copy(alpha = 0.4f)
                    ) {
                        AnimatedContent(
                            targetState = state.stickerPanelVisible,
                            label = "PanelContent",
                            modifier = Modifier.navigationBarsPadding()
                        ) { isSticker ->
                            if (isSticker) {
                                Column(Modifier.height(300.dp)) {
                                    LazyRow(
                                        Modifier.fillMaxWidth(), contentPadding = PaddingValues(
                                            horizontal = 16.dp, vertical = 16.dp
                                        ), horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(state.stickerSets, key = { it.id }) { setInfo ->
                                            val selected = state.selectedStickerSetId == setInfo.id
                                            Button(
                                                {
                                                    viewModel.selectStickerSet(
                                                        setInfo.id
                                                    )
                                                },
                                                surfaceColor = if (selected) primaryColor
                                                else surfaceContainerColor,
                                                shape = RoundedCornerShape(8.dp),
                                            ) {
                                                Text(
                                                    setInfo.title,
                                                    Modifier.padding(8.dp),
                                                    style = MiuixTheme.textStyles.footnote1,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }

                                    if (state.stickersLoading) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                tdString("Loading"),
                                                style = MiuixTheme.textStyles.footnote1
                                            )
                                        }
                                    } else {
                                        LazyVerticalGrid(
                                            columns = GridCells.Adaptive(72.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .padding(horizontal = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(
                                                state.currentSetStickers,
                                                key = { it.id }) { sticker ->
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .clip(ContinuousRoundedRectangle(8.dp))
                                                        .clickable {
                                                            viewModel.onStickerClick(
                                                                sticker
                                                            )
                                                        }
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center) {
                                                    val thumbPath = sticker.thumbnail?.fileUrl
                                                    val filePath = sticker.file.fileUrl
                                                    if (!thumbPath.isNullOrEmpty() || !filePath.isNullOrEmpty()) {
                                                        StickerBlock(
                                                            sticker, Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Text(
                                                            sticker.caption.content,
                                                            style = MiuixTheme.textStyles.title3,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(Modifier.height(320.dp)) {
                                    val camera = rememberSaveableMapViewCamera()
                                    val mapOptions = remember {
                                        MapLibreMapOptions().apply {
                                            scrollGesturesEnabled(true)
                                            zoomGesturesEnabled(true)
                                            tiltGesturesEnabled(false)
                                            rotateGesturesEnabled(false)
                                            doubleTapGesturesEnabled(true)
                                            textureMode(true)
                                        }
                                    }

                                    MapView(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        camera = camera,
                                        styleUrl = "https://tiles.openfreemap.org/styles/bright",
                                        mapOptions = mapOptions
                                    )

                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            {
                                                viewModel.hideLocationPanel()
                                            }, tint = surfaceContainerColor
                                        ) {
                                            Text(
                                                tdString("Cancel"),
                                                Modifier.padding(16.dp),
                                                style = MiuixTheme.textStyles.body1,
                                            )
                                        }
                                        Button(
                                            {
                                                val centered =
                                                    camera.value.state as? CameraState.Centered
                                                if (centered != null) {
                                                    viewModel.onSendLocation(
                                                        centered.latitude, centered.longitude
                                                    )
                                                }
                                            }, tint = primaryColor
                                        ) {
                                            Row(
                                                Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    SFIcons.Location_Fill,
                                                    null,
                                                    Modifier.size(16.dp),
                                                    Color.White
                                                )
                                                Text(
                                                    tdString("SendLocation"),
                                                    style = MiuixTheme.textStyles.body1,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        CompositionLocalProvider(
            LocalVideoDownloadProgress provides state.videoDownloadProgress,
            LocalOnDownloadVideo provides { messageId: Long ->
                viewModel.downloadVideo(messageId)
            },
            LocalDocumentDownloadProgress provides state.documentDownloadProgress,
            LocalOnDownloadDocument provides { messageId: Long ->
                viewModel.downloadDocument(messageId)
            },
            LocalCustomEmojiStickers provides state.customEmojiStickers,
            LocalOnMediaClick provides { blockId ->
                val msg = state.messages.find { msg ->
                    msg.blocks.any { it.id == blockId }
                }
                if (msg != null) {
                    navigator.push(
                        com.mocharealm.compound.ui.nav.Screen.MediaPreview(
                            chatId = msg.chatId,
                            messageId = msg.id
                        )
                    )
                }
            }
        ) {
            val onReplyClick: (Long) -> Unit = { replyMessageId ->
                viewModel.scrollToMessage(replyMessageId)
            }

            val scrollTarget = state.scrollToMessageId
            LaunchedEffect(scrollTarget, state.messages) {
                if (scrollTarget != null) {
                    val targetIdx = state.messages.indexOfFirst {
                        it.blocks.any { b -> b.id == scrollTarget }
                    }
                    if (targetIdx >= 0) {
                        val headerOffset = if (state.loadingNewer) 1 else 0
                        listState.animateScrollToItem(headerOffset + state.messages.size - 1 - targetIdx)
                        viewModel.clearScrollTarget()
                    }
                }
            }

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .layerBackdrop(layerBackdrop)
                    .fillMaxSize()
                    .then(
                        if (state.stickerPanelVisible || state.locationPanelVisible) Modifier
                        else Modifier.imeNestedScroll(focusRequester)
                    )
                    .scrollEndHaptic(),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.Top,
                overscrollEffect = null,
            ) {
                if (state.loading && state.messages.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tdString("Loading"),
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                textAlign = TextAlign.Center,
                                style = MiuixTheme.textStyles.footnote1
                            )
                        }
                    }
                } else if (state.error != null && state.messages.isEmpty()) {
                    item {
                        Card(modifier = Modifier.padding(12.dp)) {
                            BasicComponent(
                                title = tdString("ErrorOccurred"),
                                summary = state.error,
                            )
                            TextButton(
                                text = tdString("Retry"),
                                onClick = { viewModel.loadMessages() },
                                modifier = Modifier.padding(
                                    horizontal = 12.dp, vertical = 8.dp
                                ),
                            )
                        }
                    }
                } else {
                    if (state.loadingNewer) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tdString("Loading"),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    textAlign = TextAlign.Center,
                                    style = MiuixTheme.textStyles.footnote1
                                )
                            }
                        }
                    }

                    items(
                        count = state.messageItems.size,
                        key = { state.messageItems[state.messageItems.size - 1 - it].message.blocks.first().id },
                        contentType = { "Message" }) { index ->

                        val msgIndex = state.messageItems.size - 1 - index
                        val messageItem = state.messageItems[msgIndex]
                        val message = messageItem.message

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            if (messageItem.showTimestamp) {
                                TimestampLabel(timestamp = message.blocks.first().timestamp)
                            }

                            if (message.blocks.firstOrNull() is MessageBlock.SystemActionBlock) {
                                SystemMessage(
                                    message.blocks.first() as MessageBlock.SystemActionBlock
                                )
                            } else {
                                val elasticRevealState = rememberElasticRevealState(
                                    directions = setOf(RevealDirection.EndToStart),
                                    maxRevealDp = 64.dp
                                )

                                ElasticRevealSwipe(
                                    state = elasticRevealState,
                                    shape = RoundedCornerShape(0.dp),
                                    onTrigger = { direction ->
                                        if (direction == RevealDirection.EndToStart) {
                                            viewModel.setReplyingTo(message)
                                        }
                                    },
                                    swipe = { direction, progress ->
                                        if (direction == RevealDirection.EndToStart) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize(),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Icon(
                                                    imageVector = SFIcons.Arrowshape_Turn_Up_Left_Fill,
                                                    contentDescription = "Reply",
                                                    modifier = Modifier
                                                        .padding(end = 16.dp)
                                                        .size(24.dp)
                                                        .graphicsLayer {
                                                            alpha = progress
                                                        }
                                                )
                                            }
                                        }
                                    },
                                    content = { _, _ ->
                                        MessageBubble(
                                            message = message,
                                            groupPosition = messageItem.position,
                                            showAvatar = state.chatInfo?.type == ChatType.GROUP,
                                            onReplyClick = onReplyClick,
//                                            modifier = Modifier.background(surfaceColor).fillMaxWidth()
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (state.loadingMore) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = tdString("Loading"),
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    textAlign = TextAlign.Center,
                                    style = MiuixTheme.textStyles.footnote1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun uriToShareFileInfo(context: Context, uri: Uri): ShareFileInfo? {
    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

    // Resolve the original display name for the file
    val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
    }

    // Build a cache filename that preserves the original name
    val fileName = if (!displayName.isNullOrBlank()) {
        displayName
    } else {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
        "file_${System.nanoTime()}${if (ext.isNotEmpty()) ".$ext" else ""}"
    }

    val cacheDir = File(context.cacheDir, "share_files").also { it.mkdirs() }
    val cacheFile = File(cacheDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        cacheFile.outputStream().use { output -> input.copyTo(output) }
    } ?: return null

    return ShareFileInfo(
        filePath = cacheFile.absolutePath,
        mimeType = mimeType,
    )
}
