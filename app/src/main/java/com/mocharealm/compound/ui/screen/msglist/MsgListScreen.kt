package com.mocharealm.compound.ui.screen.msglist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mocharealm.compound.ui.composable.base.Avatar
import com.mocharealm.compound.ui.composable.chat.RichText
import com.mocharealm.compound.ui.screen.chat.LocalCustomEmojiStickers
import com.mocharealm.compound.ui.util.formatMessageTimestamp
import com.mocharealm.compound.ui.util.toAnnotatedString
import com.mocharealm.compound.ui.util.toPreviewAnnotatedString
import com.mocharealm.gaze.icons.Archivebox
import com.mocharealm.gaze.icons.Pin
import com.mocharealm.gaze.icons.Pin_Slash
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.RevealDirection
import com.mocharealm.gaze.ui.composable.RevealSwipe
import com.mocharealm.gaze.ui.composable.rememberRevealState
import com.mocharealm.gaze.ui.composable.resetAnimated
import com.mocharealm.tci18n.core.tdString
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MsgListScreen(
    padding: PaddingValues,
    onChatClick: (Long) -> Unit = { _ -> },
    onOpenArchived: (() -> Unit)? = null,
    viewModel: MsgListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var pullOffsetPx by remember { mutableFloatStateOf(0f) }

    CompositionLocalProvider(
        LocalCustomEmojiStickers provides state.customEmojiStickers
    ) {
        val shouldLoadMore by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 25
            }
        }

        val density = LocalDensity.current
        val maxPullPx = with(density) { 88.dp.toPx() }
        val triggerPullPx = with(density) { 62.dp.toPx() }

        val paddingPx = with(density) { 28.dp.toPx() }
        val avatarPx = with(density) { 45.dp.toPx() }
        val spacePx = with(density) { 12.dp.toPx() }

        val separatorColor = (if (isSystemInDarkTheme()) Color.White else Color.Black).copy(0.12f)

        val pullConnection = remember(onOpenArchived, listState, maxPullPx, triggerPullPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (onOpenArchived == null || source != NestedScrollSource.Drag) return Offset.Zero

                    val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                    if (available.y > 0f && atTop) {
                        val next = (pullOffsetPx + available.y).coerceAtMost(maxPullPx)
                        val consumed = next - pullOffsetPx
                        pullOffsetPx = next
                        return Offset(0f, consumed)
                    }

                    if (available.y < 0f && pullOffsetPx > 0f) {
                        val next = (pullOffsetPx + available.y).coerceAtLeast(0f)
                        val consumed = pullOffsetPx - next
                        pullOffsetPx = next
                        return Offset(0f, -consumed)
                    }

                    return Offset.Zero
                }

                override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                    if (onOpenArchived != null && pullOffsetPx >= triggerPullPx) {
                        onOpenArchived.invoke()
                    }
                    pullOffsetPx = 0f
                    return androidx.compose.ui.unit.Velocity.Zero
                }
            }
        }

        LaunchedEffect(shouldLoadMore, state.loadingMore, state.loading) {
            if (shouldLoadMore && !state.loadingMore && !state.loading && state.hasMore) {
                viewModel.loadMoreChats()
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .scrollEndHaptic()
                .nestedScroll(pullConnection),
            contentPadding =
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 12.dp,
                ),
            overscrollEffect = null,
        ) {
            if (onOpenArchived != null) {
                item(key = "archived_pulldown_entry") {
                    val revealHeight = with(density) { pullOffsetPx.toDp() }
                    val progress = (pullOffsetPx / triggerPullPx).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(revealHeight)
                            .clickable(enabled = progress >= 1f) {
                                onOpenArchived.invoke()
                                pullOffsetPx = 0f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pullOffsetPx > 0f) {
                            Text(
                                text = if (progress >= 1f) tdString("AccReleaseForArchive") else tdString("AccSwipeForArchive"),
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                modifier = Modifier.alpha(0.6f + progress * 0.4f)
                            )
                        }
                    }
                }
            }

            if (state.loading && state.chats.isEmpty()) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        BasicComponent(title = tdString("Loading"))
                    }
                }
            } else if (state.error != null && state.chats.isEmpty()) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        BasicComponent(
                            title = tdString("ErrorOccurred"),
                            summary = state.error,
                        )
                        TextButton(
                            text = tdString("Retry"),
                            onClick = viewModel::loadChats,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else if (state.chats.isEmpty()) {
                item {
                    Card(modifier = Modifier.padding(12.dp)) {
                        BasicComponent(title = "No chats found")
                        TextButton(
                            text = tdString("Refresh"),
                            onClick = viewModel::loadChats,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            } else {
                items(state.chats, key = { chat -> chat.id }) { chat ->
                    val revealState = rememberRevealState(key = chat.id)
                    RevealSwipe(
                        state = revealState,
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.animateItem(),
                        swipe = { direction, _ ->
                            if (direction == RevealDirection.StartToEnd) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MiuixTheme.colorScheme.primary)
                                        .clickable {
                                            viewModel.togglePin(chat.id, !chat.isPinned)
                                            scope.launch { revealState.resetAnimated() }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (chat.isPinned) SFIcons.Pin_Slash else SFIcons.Pin,
                                        contentDescription = "Pin",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Gray)
                                        .clickable {
                                            viewModel.toggleArchive(chat.id, !chat.isArchived)
                                            scope.launch { revealState.resetAnimated() }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = SFIcons.Archivebox,
                                        contentDescription = "Archive",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    ) { _, _ ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(MiuixTheme.colorScheme.surface)
                                .drawWithCache {
                                    onDrawBehind {
                                        drawLine(
                                            separatorColor,
                                            start =
                                                Offset(
                                                    paddingPx + avatarPx + spacePx,
                                                    size.height
                                                ),
                                            end = Offset(size.width - paddingPx, size.height),
                                            strokeWidth = 1f
                                        )
                                    }
                                }
                                .clickable {
                                    onChatClick(chat.id)
                                }
                                .padding(28.dp, 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(
                                modifier = Modifier.size(45.dp),
                                photoPath = chat.photoUrl,
                                initials = chat.title.content.take(2)
                            )
                            Column(Modifier.weight(1f)) {
                                Row {
                                    RichText(
                                        text = remember(chat.title, chat.isPinned) {
                                            if (chat.isPinned) {
                                                buildAnnotatedString {
                                                    append("📌 ")
                                                    append(chat.title.toAnnotatedString())
                                                }
                                            } else {
                                                chat.title.toAnnotatedString()
                                            }
                                        },
                                        style = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                        isInteractive = false
                                    )
                                    Text(
                                        text = chat.lastMessageDate.formatMessageTimestamp(),
                                        style = MiuixTheme.textStyles.body1,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.alpha(0.6f)
                                    )
                                }
                                RichText(
                                    text = chat.lastMessage?.toPreviewAnnotatedString(chat)
                                        ?: AnnotatedString(""),
                                    style = MiuixTheme.textStyles.body1,
                                    modifier = Modifier.alpha(0.6f),
                                    maxLines = 2,
                                    minLines = 2,
                                    isInteractive = false
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
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = tdString("Loading"),
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        }
                    }
                }
            }
        }
    }
}
