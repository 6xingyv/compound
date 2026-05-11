package com.mocharealm.compound.ui.screen.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.ui.composable.chat.MessageBubble
import com.mocharealm.compound.ui.composable.chat.SystemMessage
import com.mocharealm.compound.ui.composable.chat.TimestampLabel
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.screen.chat.composable.ChatBottomBar
import com.mocharealm.compound.ui.screen.chat.composable.ChatTopBar
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.icons.Arrowshape_Turn_Up_Left_Fill
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.ElasticRevealSwipe
import com.mocharealm.gaze.ui.composable.RevealDirection
import com.mocharealm.gaze.ui.composable.rememberElasticRevealState
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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

    BackHandler(enabled = state.editingMessage != null) {
        viewModel.cancelEditing()
    }

    LaunchedEffect(state.stickerPanelVisible, state.locationPanelVisible) {
        if (state.stickerPanelVisible || state.locationPanelVisible) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
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
            ChatTopBar(
                state = state,
                viewModel = viewModel,
                layerBackdrop = layerBackdrop
            )
        },
        bottomBar = {
            ChatBottomBar(
                state = state,
                viewModel = viewModel,
                layerBackdrop = layerBackdrop,
                focusRequester = focusRequester
            )
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
                        else Modifier.imeNestedScroll()
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
                                            onReply = { viewModel.setReplyingTo(it) },
                                            onDelete = { viewModel.deleteMessage(it) },
                                            onEdit = { viewModel.startEditing(it) },
                                            layerBackdrop = layerBackdrop,
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

