package com.mocharealm.compound.ui.screen.msglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MsgListUiState(
    val chats: List<Chat> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class MsgListViewModel(
    private val getChats: GetChatsUseCase,
    private val downloadFile: DownloadFileUseCase,
    private val subscribeToMessageUpdates: SubscribeToMessageUpdatesUseCase,
    private val getChat: GetChatUseCase
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 20
    }

    private val _uiState = MutableStateFlow(MsgListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadChats()
        viewModelScope.launch {
            subscribeToMessageUpdates().collect { event ->
                when (event) {
                    is MessageUpdateEvent.NewMessage -> {
                        val message = event.message
                        val chats = _uiState.value.chats.toMutableList()
                        val index = chats.indexOfFirst { it.id == message.chatId }
                        if (index != -1) {
                            val chat = chats[index]
                            chats.removeAt(index)
                            chats.add(0, chat.copy(
                                lastMessage = message.content,
                                lastMessageDate = message.timestamp,
                                unreadCount = if (message.isOutgoing) chat.unreadCount else chat.unreadCount + 1
                            ))
                            _uiState.update { it.copy(chats = chats) }
                        } else {
                            // Fetch new chat
                            getChat(message.chatId).onSuccess { chat ->
                                _uiState.update { state ->
                                    val newChats = state.chats.toMutableList()
                                    // Check again in case it was added
                                    if (newChats.none { it.id == chat.id }) {
                                        newChats.add(0, chat)
                                    }
                                    state.copy(chats = newChats)
                                }
                                downloadMissingPhotos(listOf(chat))
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, hasMore = true) }
            getChats(PAGE_SIZE)
                .fold(
                    onSuccess = { chats ->
                        _uiState.update {
                            it.copy(
                                chats = chats,
                                loading = false,
                                hasMore = chats.size >= PAGE_SIZE,
                            )
                        }
                        downloadMissingPhotos(chats)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, loading = false) }
                    }
                )
        }
    }

    /**
     * 滚动到底部时加载更多聊天
     */
    fun loadMoreChats() {
        val state = _uiState.value
        if (state.loadingMore || state.loading || !state.hasMore) return
        val lastChatId = state.chats.lastOrNull()?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChats(PAGE_SIZE, offsetChatId = lastChatId)
                .fold(
                    onSuccess = { moreChats ->
                        if (moreChats.isEmpty()) {
                            _uiState.update { it.copy(loadingMore = false, hasMore = false) }
                        } else {
                            val existingIds = state.chats.map { it.id }.toSet()
                            val newChats = moreChats.filter { it.id !in existingIds }
                            _uiState.update {
                                it.copy(
                                    chats = it.chats + newChats,
                                    loadingMore = false,
                                    hasMore = moreChats.size >= PAGE_SIZE,
                                )
                            }
                            downloadMissingPhotos(newChats)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(loadingMore = false, error = error.message) }
                    }
                )
        }
    }

    private fun downloadMissingPhotos(chats: List<Chat>) {
        for (chat in chats) {
            val fileId = chat.photoFileId ?: continue
            if (chat.photoUrl != null) continue
            viewModelScope.launch {
                downloadFile(fileId).onSuccess { path ->
                    _uiState.update { state ->
                        val updated = state.chats.toMutableList()
                        val idx = updated.indexOfFirst { it.id == chat.id }
                        if (idx >= 0) {
                            updated[idx] = updated[idx].copy(photoUrl = path)
                        }
                        state.copy(chats = updated)
                    }
                }
            }
        }
    }

    /**
     * 刷新聊天列表（保留已有列表，静默更新）
     * 用于从 ChatScreen 返回时同步最新消息
     */
    fun refreshChats() {
        viewModelScope.launch {
            val currentChats = _uiState.value.chats
            if (currentChats.isEmpty()) return@launch

            // 重新获取与当前列表相同数量的聊天
            getChats(currentChats.size)
                .onSuccess { freshChats ->
                    _uiState.update { state ->
                        // 保留已下载的 photoUrl
                        val existingPhotos = state.chats.associate { it.id to it.photoUrl }
                        val merged = freshChats.map { chat ->
                            val existingUrl = existingPhotos[chat.id]
                            if (existingUrl != null && chat.photoUrl == null) chat.copy(photoUrl = existingUrl) else chat
                        }
                        state.copy(chats = merged)
                    }
                    downloadMissingPhotos(freshChats)
                }
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val date = Date(timestamp * 1000) // TDLib uses seconds
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        val today = Calendar.getInstance()
        today.time = date

        return when {
            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            }

            calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) - today.get(Calendar.DAY_OF_YEAR) == 1 -> {
                "Yesterday"
            }

            else -> {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            }
        }
    }
}