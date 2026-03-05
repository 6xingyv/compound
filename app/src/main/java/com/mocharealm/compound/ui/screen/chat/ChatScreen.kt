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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.mocharealm.compound.ui.EmptyIndication
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.composable.chat.MessageBubble
import com.mocharealm.compound.ui.composable.chat.StickerBlock
import com.mocharealm.compound.ui.composable.chat.SystemMessage
import com.mocharealm.compound.ui.composable.chat.TimestampLabel
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.util.MarkdownTransformation
import com.mocharealm.compound.ui.util.PaddingValuesSide
import com.mocharealm.compound.ui.util.takeOnly
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.glassy.liquid.effect.shadow.Shadow
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.nav.LocalBackButtonVisibility
import com.mocharealm.gaze.ui.animation.InteractiveHighlight
import com.mocharealm.gaze.ui.composable.Button
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.gaze.ui.composable.OverlayPositionProvider
import com.mocharealm.gaze.ui.composable.PopupMenu
import com.mocharealm.gaze.ui.composable.TextField
import com.mocharealm.gaze.ui.layout.imeNestedScroll
import com.mocharealm.gaze.ui.layout.imePadding
import com.mocharealm.tci18n.core.tdString
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

@OptIn(ExperimentalLayoutApi::class)
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
            lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
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
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        val files = uris.mapNotNull { uri -> uriToShareFileInfo(context, uri) }
        viewModel.sendSelectedFiles(files)
    }

    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val files = uris.mapNotNull { uri -> uriToShareFileInfo(context, uri) }
        viewModel.sendSelectedFiles(files)
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.loading && state.hasMore) {
            viewModel.loadOlderMessages()
        }
    }

    val captionBar = WindowInsets.captionBar.asPaddingValues()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                Modifier
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
                    .padding(captionBar.takeOnly(PaddingValuesSide.Top))
            ) {
                Row(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (LocalBackButtonVisibility.current) {
                        LiquidSurface(
                            layerBackdrop,
                            Modifier.size(48.dp),
                            Modifier.clickable { navigator.pop() },
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
                        ) {
                            Icon(
                                SFIcons.Chevron_Backward, null, Modifier.align(Alignment.Center)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }

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
                            initials = chatInfo.title.take(2),
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
                                Text(
                                    chatInfo.title,
                                    style = MiuixTheme.textStyles.footnote1,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
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
                                    0f to surfaceColor.copy(0f),
                                    1f to surfaceColor.copy(1f)
                                )
                            )
                        }
                    }
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier

                        .then(
                            if (state.stickerPanelVisible || state.locationPanelVisible) Modifier
                            else Modifier
                                .then(if (WindowInsets.isImeVisible) Modifier else Modifier.navigationBarsPadding())
                                .padding(captionBar.takeOnly(PaddingValuesSide.Bottom))
                        )
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(Modifier.width(16.dp))
                    Box(Modifier.size(48.dp)) {
                        androidx.compose.animation.AnimatedVisibility(
                            !menuOpened.value, Modifier.dropShadow(CircleShape) {
                                radius = 24f.dp.toPx()
                                offset = Offset(0.dp.toPx(), 0.dp.toPx())
                                color = Color.Black.copy(alpha = 0.1f)
                            }, enter = fadeIn(), exit = fadeOut()
                        ) {
                            LiquidSurface(
                                layerBackdrop,
                                Modifier.fillMaxSize(),
                                Modifier.clickable { menuOpened.value = true },
                                effects = {
                                    vibrancy()
                                    blur(1.dp.toPx())
                                    lens(16.dp.toPx(), 32.dp.toPx(), chromaticAberration = false)
                                },
                                shadow = {
                                    Shadow(
                                        radius = 0.dp,
                                        offset = DpOffset(0.dp, 0.dp),
                                        color = Color.Transparent,
                                        alpha = 1f,
                                        blendMode = DrawScope.DefaultBlendMode
                                    )
                                },
                                surfaceColor = surfaceContainerColor.copy(alpha = 0.6f)
                            ) { Icon(SFIcons.Plus, null, Modifier.align(Alignment.Center)) }
                        }
                        PopupMenu(
                            menuOpened,
                            layerBackdrop,
                            popupPositionProvider = OverlayPositionProvider,
                            alignment = PopupPositionProvider.Align.BottomStart,
                            surfaceColor = surfaceContainerColor.copy(0.4f),
                            onDismissRequest = { menuOpened.value = false },
                            effects = {
                                blur(8.dp.toPx())
                                lens(16.dp.toPx(), 32.dp.toPx(), chromaticAberration = false)
                            }) {
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
                                        tdString("ChatGallery"), SFIcons.Photo_On_Rectangle_Angled
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
                                    })
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
                                                            0f to Color.Gray, 1f to Color.DarkGray
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
                                        enter = fadeIn() + slideInHorizontally { it },
                                        exit = fadeOut() + slideOutHorizontally { it }) {
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
                                        enter = fadeIn() + slideInHorizontally { -it },
                                        exit = fadeOut() + slideOutHorizontally { -it }) {
                                        TextField(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .focusRequester(focusRequester)
                                                .onFocusChanged {
                                                    if (it.isFocused) {
                                                        if (state.stickerPanelVisible) viewModel.hideStickerPanel()
                                                        if (state.locationPanelVisible) viewModel.hideLocationPanel()
                                                    }
                                                }
                                                .animateContentSize(),
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
                                                    Box {
                                                        innerTextField()
                                                        Text(
                                                            tdString(
                                                                "TypeMessage", "default"
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

                                AnimatedVisibility(
                                    visible = (viewModel.inputState.text.lines().size <= 1 || inAudioMode) && !state.loading,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
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
                        !state.loading && viewModel.inputState.text.isNotBlank(),
                        Modifier,
                        enter = fadeIn() + slideInHorizontally {
                            if (layoutDirection == LayoutDirection.Ltr) it
                            else -it
                        } + expandHorizontally(),
                        exit = fadeOut() + slideOutHorizontally {
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
                        shape = { ContinuousRoundedRectangle(topStart = 24.dp, topEnd = 24.dp) },
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
                                        Modifier
                                            .fillMaxWidth(),
                                        contentPadding = PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 16.dp
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(state.stickerSets, key = { it.id }) { setInfo ->
                                            val selected = state.selectedStickerSetId == setInfo.id
                                            Button(
                                                {
                                                    viewModel.selectStickerSet(
                                                        setInfo.id
                                                    )
                                                },
                                                surfaceColor =
                                                    if (selected) primaryColor
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
                                                key = { it.id }
                                            ) { sticker ->
                                                Box(
                                                    modifier = Modifier
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            viewModel.onStickerClick(
                                                                sticker
                                                            )
                                                        }
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.Center) {
                                                    if (!sticker.file.fileUrl.isNullOrEmpty()) {
                                                        StickerBlock(
                                                            sticker,
                                                            Modifier.fillMaxSize(),
                                                            useTextureView = false
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
                                            },
                                            tint = surfaceContainerColor
                                        ) {
                                            Text(
                                                tdString("Cancel"),
                                                Modifier
                                                    .padding(16.dp),
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
                                            },
                                            tint = primaryColor
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
            }) {
            val onReplyClick: (Long) -> Unit = { replyMessageId ->
                viewModel.scrollToMessage(replyMessageId)
            }

            val scrollTarget = state.scrollToMessageId
            LaunchedEffect(scrollTarget, state.chatItems) {
                if (scrollTarget != null) {
                    val targetIdx = state.chatItems.indexOfFirst {
                        it is MessageItem && it.message.blocks.any { b -> b.id == scrollTarget }
                    }
                    if (targetIdx >= 0) {
                        listState.animateScrollToItem(targetIdx)
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
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    }
                } else {
                    items(
                        count = state.chatItems.size,
                        key = { state.chatItems[it].key },
                        contentType = { state.chatItems[it]::class.simpleName }) { index ->
                        when (val item = state.chatItems[index]) {
                            is TimestampItem -> {
                                TimestampLabel(timestamp = item.timestamp)
                            }

                            is MessageItem -> {
                                val message = item.message
                                if (message.blocks.firstOrNull() is MessageBlock.SystemActionBlock) {
                                    SystemMessage(
                                        message.blocks.first() as MessageBlock.SystemActionBlock
                                    )
                                } else {
                                    MessageBubble(
                                        message = message,
                                        groupPosition = item.groupPosition,
                                        showAvatar = state.chatInfo?.type == ChatType.GROUP,
                                        onReplyClick = onReplyClick,
                                    )
                                }
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
