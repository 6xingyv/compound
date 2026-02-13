package com.mocharealm.compound.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val initialLoaded: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val getChatMessages: GetChatMessagesUseCase,
    private val downloadFile: DownloadFileUseCase
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 30
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var currentChatId: Long = 0

    /**
     * 加载消息：先从本地缓存加载，再从网络获取最新消息
     */
    fun loadMessages(chatId: Long) {
        currentChatId = chatId
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, hasMore = true, initialLoaded = false) }

            // ① 先加载本地缓存消息（立即显示）
            val localResult = getChatMessages(chatId, PAGE_SIZE, onlyLocal = true)
            localResult.onSuccess { localMessages ->
                if (localMessages.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            messages = localMessages,
                            loading = false,
                            initialLoaded = true,
                        )
                    }
                    downloadMissingFiles(localMessages)
                }
            }

            // ② 再从网络加载最新消息（覆盖/补充）
            getChatMessages(chatId, PAGE_SIZE)
                .fold(
                    onSuccess = { networkMessages ->
                        _uiState.update { state ->
                            // 合并：网络消息为准，保留已有 fileUrl
                            val existingFileUrls = state.messages.associate { it.id to it.fileUrl }
                            val merged = networkMessages.map { msg ->
                                val existingUrl = existingFileUrls[msg.id]
                                if (existingUrl != null && msg.fileUrl == null) msg.copy(fileUrl = existingUrl) else msg
                            }
                            state.copy(
                                messages = merged,
                                loading = false,
                                hasMore = networkMessages.size >= PAGE_SIZE,
                                initialLoaded = true,
                            )
                        }
                        downloadMissingFiles(networkMessages)
                    },
                    onFailure = { error ->
                        // 网络失败时，如果本地已有数据就保留，只清 loading
                        _uiState.update { state ->
                            if (state.messages.isEmpty()) {
                                state.copy(error = error.message, loading = false)
                            } else {
                                state.copy(loading = false, initialLoaded = true)
                            }
                        }
                    }
                )
        }
    }

    /**
     * 向上滚动加载更旧的消息
     */
    fun loadOlderMessages() {
        val state = _uiState.value
        if (state.loadingMore || state.loading || !state.hasMore) return
        val oldestMessageId = state.messages.firstOrNull()?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(currentChatId, PAGE_SIZE, fromMessageId = oldestMessageId)
                .fold(
                    onSuccess = { olderMessages ->
                        if (olderMessages.isEmpty()) {
                            _uiState.update { it.copy(loadingMore = false, hasMore = false) }
                        } else {
                            // 去重：过滤掉已有的消息
                            val existingIds = state.messages.map { it.id }.toSet()
                            val newMessages = olderMessages.filter { it.id !in existingIds }
                            _uiState.update {
                                it.copy(
                                    messages = newMessages + it.messages,
                                    loadingMore = false,
                                    hasMore = olderMessages.size >= PAGE_SIZE,
                                )
                            }
                            downloadMissingFiles(newMessages)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(loadingMore = false, error = error.message) }
                    }
                )
        }
    }

    private fun downloadMissingFiles(messages: List<Message>) {
        val mediaTypes = setOf(MessageType.PHOTO, MessageType.STICKER)
        for (msg in messages) {
            val fileId = msg.fileId ?: continue
            if (msg.fileUrl != null) continue
            if (msg.messageType !in mediaTypes) continue
            viewModelScope.launch {
                downloadFile(fileId).onSuccess { path ->
                    _uiState.update { state ->
                        val updated = state.messages.toMutableList()
                        val idx = updated.indexOfFirst { it.id == msg.id }
                        if (idx >= 0) {
                            updated[idx] = updated[idx].copy(fileUrl = path)
                        }
                        state.copy(messages = updated)
                    }
                }
            }
        }
    }
}