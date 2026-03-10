package com.mocharealm.compound.ui.screen.chat

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.StickerSetInfo
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.usecase.CloseChatUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileWithProgressUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.domain.usecase.GetChatReadPositionUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetCustomEmojiStickersUseCase
import com.mocharealm.compound.domain.usecase.GetInstalledStickerSetsUseCase
import com.mocharealm.compound.domain.usecase.GetStickerSetStickersUseCase
import com.mocharealm.compound.domain.usecase.OpenChatUseCase
import com.mocharealm.compound.domain.usecase.SaveChatReadPositionUseCase
import com.mocharealm.compound.domain.usecase.SendFilesUseCase
import com.mocharealm.compound.domain.usecase.SendLocationUseCase
import com.mocharealm.compound.domain.usecase.SendMessageUseCase
import com.mocharealm.compound.domain.usecase.SendStickerUseCase
import com.mocharealm.compound.domain.usecase.SetChatDraftMessageUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GroupPosition {
    FIRST, MIDDLE, LAST, SINGLE
}

data class ChatMessageItem(
    val message: Message,
    val position: GroupPosition,
    val showTimestamp: Boolean
)

fun List<Message>.toMessageItems(): List<ChatMessageItem> {
    return this.mapIndexed { index, message ->
        val prevMessage = this.getOrNull(index - 1)
        val nextMessage = this.getOrNull(index + 1)

        val currentTs = message.blocks.first().timestamp
        val prevTs = prevMessage?.blocks?.first()?.timestamp ?: 0L
        val nextTs = nextMessage?.blocks?.first()?.timestamp ?: 0L
        val showTimestamp = prevTs == 0L || currentTs - prevTs > 300

        val sameAbove =
            prevMessage?.sender?.id == message.sender.id &&
                    prevMessage.blocks.first() !is MessageBlock.SystemActionBlock &&
                    !showTimestamp
        val sameBelow =
            nextMessage?.sender?.id == message.sender.id &&
                    nextMessage.blocks.first() !is MessageBlock.SystemActionBlock &&
                    nextTs != 0L && nextTs - currentTs <= 300

        val position = when {
            message.blocks.first() is MessageBlock.SystemActionBlock -> GroupPosition.SINGLE
            !sameAbove && !sameBelow -> GroupPosition.SINGLE
            !sameAbove -> GroupPosition.FIRST
            !sameBelow -> GroupPosition.LAST
            else -> GroupPosition.MIDDLE
        }

        ChatMessageItem(message, position, showTimestamp)
    }
}

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val messageItems: List<ChatMessageItem> = emptyList(),
    val chatInfo: Chat? = null,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val loadingNewer: Boolean = false,
    val hasMoreNewer: Boolean = true,
    val initialLoaded: Boolean = false,
    val error: String? = null,
    val scrollToMessageId: Long? = null,
    val videoDownloadProgress: Map<Long, Int> = emptyMap(),
    val customEmojiStickers: Map<Long, MessageBlock.StickerBlock> = emptyMap(),
    val stickerSets: List<StickerSetInfo> = emptyList(),
    val currentSetStickers: List<MessageBlock.StickerBlock> = emptyList(),
    val selectedStickerSetId: Long? = null,
    val stickerPanelVisible: Boolean = false,
    val stickersLoading: Boolean = false,
    val locationPanelVisible: Boolean = false,
    val selectedFiles: List<ShareFileInfo> = emptyList(),
) {
    fun withMessages(newMessages: List<Message>): ChatUiState {
        return copy(
            messages = newMessages,
            messageItems = newMessages.toMessageItems()
        )
    }
}

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
    private val getChat: GetChatUseCase,
    private val openChat: OpenChatUseCase,
    private val closeChat: CloseChatUseCase,
    private val getInstalledStickerSets: GetInstalledStickerSetsUseCase,
    private val getStickerSetStickers: GetStickerSetStickersUseCase,
    private val sendSticker: SendStickerUseCase,
    private val sendLocation: SendLocationUseCase,
    private val sendFiles: SendFilesUseCase,
    private val setChatDraftMessage: SetChatDraftMessageUseCase,
    private val saveChatReadPosition: SaveChatReadPositionUseCase,
    private val getChatReadPosition: GetChatReadPositionUseCase,
    private val getCustomEmojiStickers: GetCustomEmojiStickersUseCase
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 50
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    val inputState = TextFieldState()

    init {
        viewModelScope.launch {
            openChat(chatId)
        }
        loadMessages()
        loadChatInfo()
        viewModelScope.launch {
            subscribeToMessageUpdates().collect { event ->
                when (event) {
                    is MessageUpdateEvent.NewMessage -> {
                        if (event.message.chatId == chatId) {
                            updateMessagesState { currentMessages ->
                                if (currentMessages.any { it.primaryId == event.message.primaryId }) {
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
                                val index = currentMessages.indexOfFirst {
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
                                val oldIndex = currentMessages.indexOfFirst {
                                    it.primaryId == event.oldMessageId
                                }
                                if (oldIndex != -1) {
                                    val updated = currentMessages.toMutableList()
                                    updated[oldIndex] = event.message
                                    updated
                                } else {
                                    if (currentMessages.any {
                                            it.primaryId == event.message.primaryId
                                        }) {
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

    @OptIn(FlowPreview::class)
    private fun loadChatInfo() {
        viewModelScope.launch {
            getChat(chatId).onSuccess { chat ->
                _uiState.update { it.copy(chatInfo = chat) }
                if (!chat.draftMessage.isNullOrEmpty() && inputState.text.isEmpty()) {
                    inputState.edit { replace(0, length, chat.draftMessage) }
                }

                // Debounce to save draft
                launch {
                    snapshotFlow { inputState.text }
                        .drop(1) // drop the initial empty or restored value
                        .map { it.toString() }
                        .debounce(1000L)
                        .collect { text ->
                            setChatDraftMessage(chatId, 0L, text)
                        }
                }
            }
        }
    }

    fun sendMessage() {
        val text = inputState.text.toString()
        val files = _uiState.value.selectedFiles

        if (text.isBlank() && files.isEmpty()) return

        if (files.isNotEmpty()) {
            viewModelScope.launch {
                sendFiles(chatId, files, text).onSuccess {
                    inputState.clearText()
                    _uiState.update { it.copy(selectedFiles = emptyList()) }
                }.onFailure { e -> }
            }
        } else {
            viewModelScope.launch {
                sendMessage(chatId, text).onSuccess { inputState.clearText() }.onFailure { e -> }
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
                    hasMoreNewer = true,
                    initialLoaded = false
                )
            }

            val readPos = getChatReadPosition(chatId)
            val offset = if (readPos > 0) -PAGE_SIZE / 2 else 0

            val localResult = getChatMessages(
                chatId,
                PAGE_SIZE,
                fromMessageId = readPos,
                offset = offset,
                onlyLocal = true
            )
            localResult.onSuccess { localMessages ->
                if (localMessages.isNotEmpty()) {
                    _uiState.update {
                        it.withMessages(localMessages.sortedBy { it.primaryTimestamp }).copy(
                            loading = false,
                            initialLoaded = true,
                            scrollToMessageId = if (readPos > 0) readPos else null
                        )
                    }
                    downloadMissingFiles(localMessages)
                }
            }

            getChatMessages(chatId, PAGE_SIZE, fromMessageId = readPos, offset = offset).fold(
                onSuccess = { networkMessages ->
                    _uiState.update { state ->
                        // Merge file URLs from already-downloaded local messages
                        val existingFileUrls = state.messages.flatMap { msg ->
                            msg.blocks.filterIsInstance<MessageBlock.MediaBlock>()
                                .filter { it.file.fileUrl != null }
                                .map { it.id to it.file.fileUrl!! }
                        }.toMap()

                        val merged = networkMessages.map { msg ->
                            msg.copy(
                                blocks = msg.blocks.map { block ->
                                    if (block is MessageBlock.MediaBlock && block.file.fileUrl == null) {
                                        existingFileUrls[block.id]?.let { url ->
                                            block.copy(
                                                file = block.file.copy(
                                                    fileUrl = url
                                                )
                                            )
                                        } ?: block
                                    } else block
                                })
                        }

                        state.withMessages(merged.sortedBy { it.primaryTimestamp }).copy(
                            loading = false,
                            hasMore = networkMessages.size >= PAGE_SIZE,
                            initialLoaded = true,
                            scrollToMessageId = if (readPos > 0) readPos else state.scrollToMessageId
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
                })
        }
    }

    fun loadOlderMessages() {
        val state = _uiState.value
        if (state.loadingMore || state.loading || !state.hasMore) return

        val oldestMessageId = state.messages.firstOrNull()?.primaryId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(
                chatId,
                PAGE_SIZE,
                fromMessageId = oldestMessageId
            ).fold(onSuccess = { olderMessages ->
                if (olderMessages.isEmpty()) {
                    _uiState.update {
                        it.copy(loadingMore = false, hasMore = false)
                    }
                } else {
                    val existingIds = state.messages.map { it.primaryId }.toSet()
                    val newMessages = olderMessages.filter { it.primaryId !in existingIds }

                    val combinedMessages = if (newMessages.isNotEmpty()) {
                        newMessages.sortedBy { it.primaryTimestamp } + state.messages
                    } else {
                        state.messages
                    }

                    _uiState.update {
                        it.withMessages(combinedMessages).copy(
                            loadingMore = false,
                            hasMore = newMessages.isNotEmpty(),
                        )
                    }
                    downloadMissingFiles(newMessages)
                }
            }, onFailure = { error ->
                _uiState.update {
                    it.copy(loadingMore = false, error = error.message)
                }
            })
        }
    }

    fun loadNewerMessages() {
        val state = _uiState.value
        if (state.loadingNewer || state.loading || !state.hasMoreNewer || state.messages.isEmpty()) return

        val newestMessageId = state.messages.lastOrNull()?.primaryId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingNewer = true) }
            getChatMessages(
                chatId,
                PAGE_SIZE,
                fromMessageId = newestMessageId,
                offset = -PAGE_SIZE + 1
            ).fold(onSuccess = { newerMessages ->
                if (newerMessages.isEmpty()) {
                    _uiState.update {
                        it.copy(loadingNewer = false, hasMoreNewer = false)
                    }
                } else {
                    val existingIds = state.messages.map { it.primaryId }.toSet()
                    val newMessages = newerMessages.filter { it.primaryId !in existingIds }

                    val combinedMessages = if (newMessages.isNotEmpty()) {
                        state.messages + newMessages.sortedBy { it.primaryTimestamp }
                    } else {
                        state.messages
                    }

                    _uiState.update {
                        it.withMessages(combinedMessages).copy(
                            loadingNewer = false,
                            hasMoreNewer = newMessages.isNotEmpty(),
                        )
                    }
                    downloadMissingFiles(newMessages)
                }
            }, onFailure = { error ->
                _uiState.update { it.copy(loadingNewer = false, error = error.message) }
            })
        }
    }

    fun scrollToMessage(messageId: Long) {
        if (_uiState.value.messages.any { it.primaryId == messageId }) {
            _uiState.update { it.copy(scrollToMessageId = messageId) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChatMessages(chatId, PAGE_SIZE, fromMessageId = messageId, offset = -1).fold(
                onSuccess = { loaded ->
                    val existingIds = _uiState.value.messages.map { it.primaryId }.toSet()
                    val newMessages = loaded.filter { it.primaryId !in existingIds }
                    val combinedMessages = (newMessages + _uiState.value.messages).sortedBy {
                        it.primaryTimestamp
                    }
                    _uiState.update { state ->
                        state.withMessages(combinedMessages).copy(
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
                })
        }
    }

    fun clearScrollTarget() {
        _uiState.update { it.copy(scrollToMessageId = null) }
    }

    fun saveReadPosition(messageId: Long) {
        viewModelScope.launch {
            saveChatReadPosition(chatId, messageId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            closeChat(chatId)
        }
    }

    private fun updateMessagesState(updateFn: (List<Message>) -> List<Message>) {
        _uiState.update { state ->
            val newMessages = updateFn(state.messages)
            state.withMessages(newMessages)
        }
    }


    private fun downloadMissingFiles(messages: List<Message>) {
        for (msg in messages) {
            for (block in msg.blocks) {
                when (block) {
                    is MessageBlock.MediaBlock -> {
                        val fileId = block.file.fileId
                        if (fileId != null && block.file.fileUrl == null && block.mediaType == MessageBlock.MediaBlock.MediaType.PHOTO) {
                            viewModelScope.launch {
                                downloadFile(fileId).onSuccess { path ->
                                    updateBlockFile(msg.primaryId, block.id, path)
                                }
                            }
                        }
                        if (
                            block.thumbnail?.fileId != null
                            && block.thumbnail.fileUrl == null
                            && block.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO
                        ) {
                            viewModelScope.launch {
                                downloadFile(block.thumbnail.fileId).onSuccess { path ->
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

                    is MessageBlock.TextBlock -> {
                        val customEmojiIds = block.content.entities
                            .filter { it.type is Text.TextEntityType.CustomEmoji }
                            .map { (it.type as Text.TextEntityType.CustomEmoji).customEmojiId }
                            .filter { id -> !_uiState.value.customEmojiStickers.containsKey(id) }

                        customEmojiIds.forEach { id ->
                            viewModelScope.launch {
                                getCustomEmojiStickers(listOf(id)).onSuccess { stickers ->
                                    stickers.firstOrNull()?.let { sticker ->
                                        _uiState.update { state ->
                                            state.copy(customEmojiStickers = state.customEmojiStickers + (id to sticker))
                                        }
                                        downloadCustomEmojiFiles(listOf(sticker))
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun downloadCustomEmojiFiles(stickers: List<MessageBlock.StickerBlock>) {
        for (sticker in stickers) {
            val fileId = sticker.file.fileId
            if (fileId != null && sticker.file.fileUrl == null) {
                viewModelScope.launch {
                    downloadFile(fileId).onSuccess { path ->
                        _uiState.update { state ->
                            state.copy(
                                customEmojiStickers = state.customEmojiStickers.mapValues { (_, s) ->
                                    if (s.file.fileId == fileId) {
                                        s.copy(file = s.file.copy(fileUrl = path))
                                    } else s
                                }
                            )
                        }
                    }
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
                        blocks = msg.blocks.map { block ->
                            when (block) {
                                is MessageBlock.MediaBlock -> if (block.id == blockId) block.copy(
                                    file = block.file.copy(fileUrl = path)
                                ) else block

                                is MessageBlock.StickerBlock -> if (block.id == blockId) block.copy(
                                    file = block.file.copy(fileUrl = path)
                                ) else block

                                else -> block
                            }
                        })
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
                        blocks = msg.blocks.map { block ->
                            when {
                                block is MessageBlock.MediaBlock && block.id == blockId -> block.copy(
                                    thumbnail = block.thumbnail?.copy(
                                        fileUrl = path
                                    )
                                )

                                else -> block
                            }
                        })
                } else msg
            }
        }
    }

    fun downloadVideo(messageId: Long) {
        val msg = _uiState.value.messages.find { it.primaryId == messageId } ?: return
        val videoBlock = msg.blocks.filterIsInstance<MessageBlock.MediaBlock>().firstOrNull {
            it.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO
        } ?: return
        val fileId = videoBlock.file.fileId ?: return
        if (videoBlock.file.fileUrl != null) return
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

    // ── Sticker panel ────────────────────────────────────────────────────

    fun showStickerPanel() {
        _uiState.update { it.copy(stickerPanelVisible = true, locationPanelVisible = false) }
        if (_uiState.value.stickerSets.isEmpty()) {
            loadStickerSets()
        }
    }

    fun hideStickerPanel() {
        _uiState.update { it.copy(stickerPanelVisible = false) }
    }

    private fun loadStickerSets() {
        viewModelScope.launch {
            _uiState.update { it.copy(stickersLoading = true) }
            getInstalledStickerSets().onSuccess { sets ->
                _uiState.update { it.copy(stickerSets = sets, stickersLoading = false) }
                // Auto-select first set
                if (sets.isNotEmpty()) {
                    selectStickerSet(sets.first().id)
                }
            }.onFailure {
                _uiState.update { it.copy(stickersLoading = false) }
            }
        }
    }

    fun selectStickerSet(setId: Long) {
        if (_uiState.value.selectedStickerSetId == setId) return
        _uiState.update { it.copy(selectedStickerSetId = setId, stickersLoading = true) }
        viewModelScope.launch {
            getStickerSetStickers(setId).onSuccess { stickers ->
                // Download thumbnails for stickers that don't have local files
                for (sticker in stickers) {
                    val thumbFileId = sticker.thumbnail?.fileId ?: sticker.file.fileId
                    if (thumbFileId != null && sticker.thumbnail?.fileUrl == null && sticker.file.fileUrl == null) {
                        launch {
                            downloadFile(thumbFileId).onSuccess { path ->
                                _uiState.update { state ->
                                    state.copy(
                                        currentSetStickers = state.currentSetStickers.map { s ->
                                            if (s.id == sticker.id) {
                                                if (thumbFileId == s.thumbnail?.fileId) {
                                                    s.copy(thumbnail = s.thumbnail.copy(fileUrl = path))
                                                } else {
                                                    s.copy(file = s.file.copy(fileUrl = path))
                                                }
                                            } else s
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                _uiState.update {
                    it.copy(currentSetStickers = stickers, stickersLoading = false)
                }
            }.onFailure {
                _uiState.update { it.copy(stickersLoading = false) }
            }
        }
    }

    fun onStickerClick(sticker: MessageBlock.StickerBlock) {
        viewModelScope.launch {
            sendSticker(chatId, sticker)
            hideStickerPanel()
        }
    }

    // ── Location ─────────────────────────────────────────────────────────

    fun showLocationPanel() {
        _uiState.update { it.copy(locationPanelVisible = true, stickerPanelVisible = false) }
    }

    fun hideLocationPanel() {
        _uiState.update { it.copy(locationPanelVisible = false) }
    }

    fun onSendLocation(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            sendLocation(chatId, latitude, longitude)
            hideLocationPanel()
        }
    }

    // ── Gallery / Document ───────────────────────────────────────────────

    fun onFilesSelected(files: List<ShareFileInfo>) {
        if (files.isEmpty()) return
        _uiState.update { it.copy(selectedFiles = it.selectedFiles + files) }
    }

    fun removeSelectedFile(file: ShareFileInfo) {
        _uiState.update { it.copy(selectedFiles = it.selectedFiles - file) }
    }
}
