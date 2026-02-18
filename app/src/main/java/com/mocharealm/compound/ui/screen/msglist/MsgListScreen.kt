package com.mocharealm.compound.ui.screen.msglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.util.formatMessageTimestamp
import com.mocharealm.tci18n.core.tdString
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MsgListScreen(
    padding: PaddingValues,
    onChatClick: (Long) -> Unit = { _ -> },
    refreshSignal: Int = 0,
    viewModel: MsgListViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // 从 ChatScreen 返回时刷新聊天列表
    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) {
            viewModel.refreshChats()
        }
    }

    // 当滚动到无法再往下滚动时加载更多聊天
    val shouldLoadMore by remember {
        derivedStateOf {
            !listState.canScrollForward && listState.layoutInfo.totalItemsCount > 0
        }
    }

    val density = LocalDensity.current

    val paddingPx = with(density) { 28.dp.toPx() }
    val avatarPx = with(density) { 45.dp.toPx() }
    val spacePx = with(density) { 12.dp.toPx() }

    val separatorColor = (if (isSystemInDarkTheme()) Color.White else Color.Black).copy(0.12f)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.loading && state.hasMore) {
            viewModel.loadMoreChats()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxHeight()
            .overScrollVertical()
            .scrollEndHaptic(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + 12.dp,
        ),
        overscrollEffect = null,
    ) {
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
            items(state.chats) { chat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onChatClick(chat.id)
                        }
                        .drawWithCache {
                            onDrawBehind {
                                drawLine(
                                    separatorColor,
                                    start = Offset(paddingPx + avatarPx + spacePx, size.height),
                                    end = Offset(size.width - paddingPx, size.height),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        .padding(28.dp, 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(
                        modifier = Modifier.size(45.dp),
                        photoPath = chat.photoUrl,
                        initials = chat.title.take(2)
                    )
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(
                                text = chat.title,
                                style = MiuixTheme.textStyles.body1,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = chat.lastMessageDate.formatMessageTimestamp(),
                                style = MiuixTheme.textStyles.body1,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.alpha(0.6f)
                            )
                        }
                        Text(
                            text = chat.lastMessage ?: "",
                            style = MiuixTheme.textStyles.body1,
                            minLines = 2,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alpha(0.6f)
                        )
                    }
                }
            }

            // 加载更多指示器
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
