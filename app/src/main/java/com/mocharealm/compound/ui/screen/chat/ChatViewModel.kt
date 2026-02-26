package com.mocharealm.compound.ui.screen.chat

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileWithProgressUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.SendMessageUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GroupPosition {
    FIRST,
    MIDDLE,
    LAST,
    SINGLE
}

sealed interface ChatItem {
    val key: String
}

data class TimestampItem(val timestamp: Long) : ChatItem {
    override val key = "ts_$timestamp"
}

data class MessageItem(val message: Message, val groupPosition: GroupPosition) : ChatItem {
    override val key = "msg_${message.blocks.first().id}"
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

/** Helper: primary message ID (first block's id) */
private val Message.primaryId: Long
    get() = blocks.first().id

/** Helper: primary timestamp (first block's timestamp) */
private val Message.primaryTimestamp: Long
    get() = blocks.first().timestamp

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
    val uiState = _uiState.asStateFlow()

    val inputState = TextFieldState()

    init {
        loadMessages()
        loadChatInfo()
        viewModelScope.launch {
            subscribeToMessageUpdates().collect { event ->
                when (event) {
                    is MessageUpdateEvent.NewMessage -> {
                        if (event.message.chatId == chatId) {
                            updateMessagesState { currentMessages ->
                                if (currentMessages.any { it.primaryId == event.message.primaryId }
                                ) {
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
                                        currentMessages.indexOfFirst {
                                            it.primaryId == event.message.primaryId
                                        }
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
                                        currentMessages.indexOfFirst {
                                            it.primaryId == event.oldMessageId
                                        }
                                if (oldIndex != -1) {
                                    val updated = currentMessages.toMutableList()
                                    updated[oldIndex] = event.message
                                    updated
                                } else {
                                    if (currentMessages.any {
                                                it.primaryId == event.message.primaryId
                                            }
                                    ) {
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
                                currentMessages.filterNot { it.primaryId == event.messageId }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadChatInfo() {
        viewModelScope.launch {
            getChat(chatId).onSuccess { chat -> _uiState.update { it.copy(chatInfo = chat) } }
        }
    }

    fun sendMessage() {
        val text = inputState.text.toString()
        if (text.isBlank()) return

        viewModelScope.launch {
            sendMessage(chatId, text).onSuccess { inputState.clearText() }.onFailure { e -> }
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, error = null, hasMore = true, initialLoaded = false)
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
                                    // Merge file URLs from already-downloaded local messages
                                    val existingFileUrls =
                                            state.messages
                                                    .flatMap { msg ->
                                                        msg.blocks
                                                                .filterIsInstance<
                                                                        MessageBlock.MediaBlock>()
                                                                .filter { it.file.fileUrl != null }
                                                                .map { it.id to it.file.fileUrl!! }
                                                    }
                                                    .toMap()

                                    val merged =
                                            networkMessages.map { msg ->
                                                msg.copy(
                                                        blocks =
                                                                msg.blocks.map { block ->
                                                                    if (block is
                                                                                    MessageBlock.MediaBlock &&
                                                                                    block.file
                                                                                            .fileUrl ==
                                                                                            null
                                                                    ) {
                                                                        existingFileUrls[block.id]
                                                                                ?.let { url ->
                                                                                    block.copy(
                                                                                            file =
                                                                                                    block.file
                                                                                                            .copy(
                                                                                                                    fileUrl =
                                                                                                                            url
                                                                                                            )
                                                                                    )
                                                                                }
                                                                                ?: block
                                                                    } else block
                                                                }
                                                )
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
        val oldestMessageId = state.messages.firstOrNull()?.primaryId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(chatId, PAGE_SIZE, fromMessageId = oldestMessageId)
                    .fold(
                            onSuccess = { olderMessages ->
                                if (olderMessages.isEmpty()) {
                                    _uiState.update {
                                        it.copy(loadingMore = false, hasMore = false)
                                    }
                                } else {
                                    val existingIds = state.messages.map { it.primaryId }.toSet()
                                    val newMessages =
                                            olderMessages.filter { it.primaryId !in existingIds }
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
                                _uiState.update {
                                    it.copy(loadingMore = false, error = error.message)
                                }
                            }
                    )
        }
    }

    fun scrollToMessage(messageId: Long) {
        if (_uiState.value.messages.any { it.primaryId == messageId }) {
            _uiState.update { it.copy(scrollToMessageId = messageId) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(chatId, PAGE_SIZE, fromMessageId = messageId, offset = -1)
                    .fold(
                            onSuccess = { loaded ->
                                val existingIds =
                                        _uiState.value.messages.map { it.primaryId }.toSet()
                                val newMessages = loaded.filter { it.primaryId !in existingIds }
                                val combinedMessages =
                                        (newMessages + _uiState.value.messages).sortedBy {
                                            it.primaryTimestamp
                                        }
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
                                _uiState.update {
                                    it.copy(loadingMore = false, error = error.message)
                                }
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
            state.copy(messages = newMessages, chatItems = computeChatItems(newMessages))
        }
    }

    /**
     * Albums are already pre-aggregated at the repo layer, so each Message in the list is either a
     * single-block message or a multi-block album.
     */
    private fun computeChatItems(messages: List<Message>): List<ChatItem> {
        val sorted = messages.sortedBy { it.primaryTimestamp }
        val items = mutableListOf<ChatItem>()
        var lastTimestamp = 0L

        for (msg in sorted) {
            val ts = msg.primaryTimestamp

            if (lastTimestamp == 0L || ts - lastTimestamp > 300) {
                items.add(TimestampItem(ts))
                lastTimestamp = ts
            }

            items.add(MessageItem(msg, GroupPosition.SINGLE))
        }

        // Compute group positions
        for (j in items.indices) {
            val current = items[j] as? MessageItem ?: continue
            val primary = current.message

            // System messages don't group
            if (primary.blocks.first() is MessageBlock.SystemActionBlock) continue

            val prevItem = items.getOrNull(j - 1) as? MessageItem
            val nextItem = items.getOrNull(j + 1) as? MessageItem

            val sameAbove =
                    prevItem?.message?.sender?.id == primary.sender.id &&
                            prevItem.message.blocks.first() !is MessageBlock.SystemActionBlock
            val sameBelow =
                    nextItem?.message?.sender?.id == primary.sender.id &&
                            nextItem.message.blocks.first() !is MessageBlock.SystemActionBlock

            // FIRST = visually topmost in group (shows sender name)
            // LAST  = visually bottommost in group (shows avatar & tail)
            // The list is reversed at the end for reverseLayout, so use
            // natural chronological order here: first in time = FIRST.
            val position =
                    when {
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
        for (msg in messages) {
            for (block in msg.blocks) {
                when (block) {
                    is MessageBlock.MediaBlock -> {
                        val fileId = block.file.fileId
                        if (fileId != null &&
                                        block.file.fileUrl == null &&
                                        block.mediaType == MessageBlock.MediaBlock.MediaType.PHOTO
                        ) {
                            viewModelScope.launch {
                                downloadFile(fileId).onSuccess { path ->
                                    updateBlockFile(msg.primaryId, block.id, path)
                                }
                            }
                        }
                        // Download thumbnail for videos
                        val thumbId = block.thumbnail?.fileId
                        if (thumbId != null &&
                                        block.thumbnail?.fileUrl == null &&
                                        block.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO
                        ) {
                            viewModelScope.launch {
                                downloadFile(thumbId).onSuccess { path ->
                                    updateBlockThumbnail(msg.primaryId, block.id, path)
                                }
                            }
                        }
                    }
                    is MessageBlock.StickerBlock -> {
                        val fileId = block.file.fileId
                        if (fileId != null && block.file.fileUrl == null) {
                            viewModelScope.launch {
                                downloadFile(fileId).onSuccess { path ->
                                    updateBlockFile(msg.primaryId, block.id, path)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /** Updates a block's file URL within a message */
    private fun updateBlockFile(messageId: Long, blockId: Long, path: String) {
        updateMessagesState { currentMessages ->
            currentMessages.map { msg ->
                if (msg.primaryId == messageId) {
                    msg.copy(
                            blocks =
                                    msg.blocks.map { block ->
                                        when {
                                            block is MessageBlock.MediaBlock &&
                                                    block.id == blockId ->
                                                    block.copy(
                                                            file = block.file.copy(fileUrl = path)
                                                    )
                                            block is MessageBlock.StickerBlock &&
                                                    block.id == blockId ->
                                                    block.copy(
                                                            file = block.file.copy(fileUrl = path)
                                                    )
                                            else -> block
                                        }
                                    }
                    )
                } else msg
            }
        }
    }

    /** Updates a block's thumbnail URL within a message */
    private fun updateBlockThumbnail(messageId: Long, blockId: Long, path: String) {
        updateMessagesState { currentMessages ->
            currentMessages.map { msg ->
                if (msg.primaryId == messageId) {
                    msg.copy(
                            blocks =
                                    msg.blocks.map { block ->
                                        when {
                                            block is MessageBlock.MediaBlock &&
                                                    block.id == blockId ->
                                                    block.copy(
                                                            thumbnail =
                                                                    block.thumbnail?.copy(
                                                                            fileUrl = path
                                                                    )
                                                    )
                                            else -> block
                                        }
                                    }
                    )
                } else msg
            }
        }
    }

    fun downloadVideo(messageId: Long) {
        val msg = _uiState.value.messages.find { it.primaryId == messageId } ?: return
        val videoBlock =
                msg.blocks.filterIsInstance<MessageBlock.MediaBlock>().firstOrNull {
                    it.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO
                }
                        ?: return
        val fileId = videoBlock.file.fileId ?: return
        if (videoBlock.file.fileUrl != null) return
        if (_uiState.value.videoDownloadProgress.containsKey(messageId)) return

        viewModelScope.launch {
            downloadFileWithProgress(fileId).collect { progress ->
                when (progress) {
                    is DownloadProgress.Downloading -> {
                        _uiState.update { state ->
                            state.copy(
                                    videoDownloadProgress =
                                            state.videoDownloadProgress +
                                                    (messageId to progress.percent)
                            )
                        }
                    }
                    is DownloadProgress.Completed -> {
                        updateBlockFile(messageId, videoBlock.id, progress.path)
                        _uiState.update { state ->
                            state.copy(
                                    videoDownloadProgress = state.videoDownloadProgress - messageId
                            )
                        }
                    }
                }
            }
        }
    }
}
