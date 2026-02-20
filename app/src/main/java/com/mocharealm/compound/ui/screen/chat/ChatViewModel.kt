package com.mocharealm.compound.ui.screen.chat

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileWithProgressUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.SendMessageUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GroupPosition { FIRST, MIDDLE, LAST, SINGLE }

sealed interface ChatItem {
    val key: String
}

data class TimestampItem(val timestamp: Long) : ChatItem {
    override val key = "ts_$timestamp"
}

data class MessageItem(
    val messages: List<Message>,
    val isAlbum: Boolean,
    val groupPosition: GroupPosition
) : ChatItem {
    override val key = "msg_${messages.first().id}"
    val primaryMessage get() = messages.first()
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val chatItems: List<ChatItem> = emptyList(),
    val chatInfo: Chat? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val initialLoaded: Boolean = false,
    val error: String? = null,
    val scrollToMessageId: Long? = null,
    val videoDownloadProgress: Map<Long, Int> = emptyMap()
)

class ChatViewModel(
    private val chatId: Long,
    private val getChatMessages: GetChatMessagesUseCase,
    private val downloadFile: DownloadFileUseCase,
    private val downloadFileWithProgress: DownloadFileWithProgressUseCase,
    private val sendMessage: SendMessageUseCase,
    private val subscribeToMessageUpdates: SubscribeToMessageUpdatesUseCase,
    private val getChat: GetChatUseCase
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 30
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    val inputState = TextFieldState()

    init {
        loadMessages()
        loadChatInfo()
        viewModelScope.launch {
            subscribeToMessageUpdates()
                .collect { event ->
                    when (event) {
                        is MessageUpdateEvent.NewMessage -> {
                            if (event.message.chatId == chatId) {
                                updateMessagesState { currentMessages ->
                                    if (currentMessages.any { it.id == event.message.id }) {
                                        currentMessages
                                    } else {
                                        currentMessages + event.message
                                    }
                                }
                                downloadMissingFiles(listOf(event.message))
                            }
                        }

                        is MessageUpdateEvent.MessageUpdated -> {
                            if (event.message.chatId == chatId) {
                                updateMessagesState { currentMessages ->
                                    val index =
                                        currentMessages.indexOfFirst { it.id == event.message.id }
                                    if (index != -1) {
                                        val updated = currentMessages.toMutableList()
                                        updated[index] = event.message
                                        updated
                                    } else {
                                        currentMessages
                                    }
                                }
                                downloadMissingFiles(listOf(event.message))
                            }
                        }

                        is MessageUpdateEvent.MessageSendSucceeded -> {
                            if (event.message.chatId == chatId) {
                                updateMessagesState { currentMessages ->
                                    val oldIndex =
                                        currentMessages.indexOfFirst { it.id == event.oldMessageId }
                                    if (oldIndex != -1) {
                                        val updated = currentMessages.toMutableList()
                                        updated[oldIndex] = event.message
                                        updated
                                    } else {
                                        if (currentMessages.any { it.id == event.message.id }) {
                                            currentMessages
                                        } else {
                                            currentMessages + event.message
                                        }
                                    }
                                }
                                downloadMissingFiles(listOf(event.message))
                            }
                        }

                        is MessageUpdateEvent.MessageDeleted -> {
                            if (event.chatId == chatId) {
                                updateMessagesState { currentMessages ->
                                    currentMessages.filterNot { it.id == event.messageId }
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun loadChatInfo() {
        viewModelScope.launch {
            getChat(chatId).onSuccess { chat ->
                _uiState.update { it.copy(chatInfo = chat) }
            }
        }
    }

    fun sendMessage() {
        val text = inputState.text.toString()
        if (text.isBlank()) return

        viewModelScope.launch {
            sendMessage(chatId, text).onSuccess {
                inputState.clearText()
            }.onFailure { e ->
            }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    error = null,
                    hasMore = true,
                    initialLoaded = false
                )
            }

            val localResult = getChatMessages(chatId, PAGE_SIZE, onlyLocal = true)
            localResult.onSuccess { localMessages ->
                if (localMessages.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            messages = localMessages,
                            chatItems = computeChatItems(localMessages),
                            loading = false,
                            initialLoaded = true,
                        )
                    }
                    downloadMissingFiles(localMessages)
                }
            }

            getChatMessages(chatId, PAGE_SIZE)
                .fold(
                    onSuccess = { networkMessages ->
                        _uiState.update { state ->
                            val existingFileUrls = state.messages.associate { it.id to it.fileUrl }
                            val merged = networkMessages.map { msg ->
                                val existingUrl = existingFileUrls[msg.id]
                                if (existingUrl != null && msg.fileUrl == null) msg.copy(fileUrl = existingUrl) else msg
                            }
                            state.copy(
                                messages = merged,
                                chatItems = computeChatItems(merged),
                                loading = false,
                                hasMore = networkMessages.size >= PAGE_SIZE,
                                initialLoaded = true,
                            )
                        }
                        downloadMissingFiles(networkMessages)
                    },
                    onFailure = { error ->
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

    fun loadOlderMessages() {
        val state = _uiState.value
        if (state.loadingMore || state.loading || !state.hasMore) return
        val oldestMessageId = state.messages.firstOrNull()?.id ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(chatId, PAGE_SIZE, fromMessageId = oldestMessageId)
                .fold(
                    onSuccess = { olderMessages ->
                        if (olderMessages.isEmpty()) {
                            _uiState.update { it.copy(loadingMore = false, hasMore = false) }
                        } else {
                            val existingIds = state.messages.map { it.id }.toSet()
                            val newMessages = olderMessages.filter { it.id !in existingIds }
                            val combinedMessages = newMessages + state.messages
                            _uiState.update {
                                it.copy(
                                    messages = combinedMessages,
                                    chatItems = computeChatItems(combinedMessages),
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

    fun scrollToMessage(messageId: Long) {
        if (_uiState.value.messages.any { it.id == messageId }) {
            _uiState.update { it.copy(scrollToMessageId = messageId) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(chatId, PAGE_SIZE, fromMessageId = messageId, offset = -1)
                .fold(
                    onSuccess = { loaded ->
                        val existingIds = _uiState.value.messages.map { it.id }.toSet()
                        val newMessages = loaded.filter { it.id !in existingIds }
                        val combinedMessages =
                            (newMessages + _uiState.value.messages).sortedBy { it.timestamp }
                        _uiState.update { state ->
                            state.copy(
                                messages = combinedMessages,
                                chatItems = computeChatItems(combinedMessages),
                                loadingMore = false,
                                scrollToMessageId = messageId
                            )
                        }
                        downloadMissingFiles(newMessages)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(loadingMore = false, error = error.message) }
                    }
                )
        }
    }

    fun clearScrollTarget() {
        _uiState.update { it.copy(scrollToMessageId = null) }
    }

    private fun updateMessagesState(updateFn: (List<Message>) -> List<Message>) {
        _uiState.update { state ->
            val newMessages = updateFn(state.messages)
            state.copy(
                messages = newMessages,
                chatItems = computeChatItems(newMessages)
            )
        }
    }

    private fun computeChatItems(messages: List<Message>): List<ChatItem> {
        val items = mutableListOf<ChatItem>()
        var lastTimestamp = 0L

        var i = 0
        while (i < messages.size) {
            val msg = messages[i]

            if (lastTimestamp == 0L || msg.timestamp - lastTimestamp > 300) {
                items.add(TimestampItem(msg.timestamp))
                lastTimestamp = msg.timestamp
            }

            val albumMessages = mutableListOf(msg)
            if (
                msg.mediaAlbumId != 0L
                && msg.messageType in listOf(
                    MessageType.PHOTO, MessageType.VIDEO,
                    MessageType.DOCUMENT
                )
            ) {
                while (i + 1 < messages.size && messages[i + 1].mediaAlbumId == msg.mediaAlbumId &&
                    (messages[i + 1].messageType == MessageType.PHOTO || messages[i + 1].messageType == MessageType.VIDEO)
                ) {
                    i++
                    albumMessages.add(messages[i])
                }
            }

            items.add(MessageItem(albumMessages, albumMessages.size > 1, GroupPosition.SINGLE))
            i++
        }

        for (j in items.indices) {
            val current = items[j] as? MessageItem ?: continue
            val primary = current.primaryMessage

            if (primary.messageType == MessageType.SYSTEM) continue
            val prevItem = items.getOrNull(j - 1) as? MessageItem
            val nextItem = items.getOrNull(j + 1) as? MessageItem

            val sameAbove =
                prevItem?.primaryMessage?.senderId == primary.senderId && prevItem.primaryMessage.messageType != MessageType.SYSTEM
            val sameBelow =
                nextItem?.primaryMessage?.senderId == primary.senderId && nextItem.primaryMessage.messageType != MessageType.SYSTEM

            val position = when {
                !sameAbove && !sameBelow -> GroupPosition.SINGLE
                !sameAbove -> GroupPosition.FIRST
                !sameBelow -> GroupPosition.LAST
                else -> GroupPosition.MIDDLE
            }
            items[j] = current.copy(groupPosition = position)
        }

        return items.reversed()
    }

    private fun downloadMissingFiles(messages: List<Message>) {
        val mediaTypes = setOf(MessageType.PHOTO, MessageType.STICKER)
        for (msg in messages) {
            val fileId = msg.fileId
            if (fileId != null && msg.fileUrl == null && msg.messageType in mediaTypes) {
                viewModelScope.launch {
                    downloadFile(fileId).onSuccess { path ->
                        updateMessagesState { currentMessages ->
                            val updated = currentMessages.toMutableList()
                            val idx = updated.indexOfFirst { it.id == msg.id }
                            if (idx >= 0) {
                                updated[idx] = updated[idx].copy(fileUrl = path)
                            }
                            updated
                        }
                    }
                }
            }
            val thumbId = msg.thumbnailFileId
            if (thumbId != null && msg.thumbnailUrl == null && msg.messageType == MessageType.VIDEO) {
                viewModelScope.launch {
                    downloadFile(thumbId).onSuccess { path ->
                        updateMessagesState { currentMessages ->
                            val updated = currentMessages.toMutableList()
                            val idx = updated.indexOfFirst { it.id == msg.id }
                            if (idx >= 0) {
                                updated[idx] = updated[idx].copy(thumbnailUrl = path)
                            }
                            updated
                        }
                    }
                }
            }
        }
    }

    fun downloadVideo(messageId: Long) {
        val msg = _uiState.value.messages.find { it.id == messageId } ?: return
        val fileId = msg.fileId ?: return
        if (msg.fileUrl != null) return
        if (_uiState.value.videoDownloadProgress.containsKey(messageId)) return

        viewModelScope.launch {
            downloadFileWithProgress(fileId).collect { progress ->
                when (progress) {
                    is DownloadProgress.Downloading -> {
                        _uiState.update { state ->
                            state.copy(
                                videoDownloadProgress = state.videoDownloadProgress + (messageId to progress.percent)
                            )
                        }
                    }

                    is DownloadProgress.Completed -> {
                        updateMessagesState { currentMessages ->
                            val updated = currentMessages.toMutableList()
                            val idx = updated.indexOfFirst { it.id == messageId }
                            if (idx >= 0) {
                                updated[idx] = updated[idx].copy(fileUrl = progress.path)
                            }
                            updated
                        }
                        _uiState.update { state ->
                            state.copy(videoDownloadProgress = state.videoDownloadProgress - messageId)
                        }
                    }
                }
            }
        }
    }
}