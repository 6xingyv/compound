package com.mocharealm.compound.ui.screen.msglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import com.mocharealm.compound.domain.usecase.ToggleChatArchiveUseCase
import com.mocharealm.compound.domain.usecase.ToggleChatPinUseCase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.Text as DomainText
import com.mocharealm.compound.domain.usecase.GetCustomEmojiStickersUseCase

data class MsgListUiState(
    val chats: List<Chat> = emptyList(),
    val customEmojiStickers: Map<Long, MessageBlock.StickerBlock> = emptyMap(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

class MsgListViewModel(
    private val getChats: GetChatsUseCase,
    private val downloadFile: DownloadFileUseCase,
    private val subscribeToMessageUpdates: SubscribeToMessageUpdatesUseCase,
    private val getChat: GetChatUseCase,
    private val toggleChatPin: ToggleChatPinUseCase,
    private val toggleChatArchive: ToggleChatArchiveUseCase,
    private val getCustomEmojiStickers: GetCustomEmojiStickersUseCase
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
                        val chatId = message.chatId

                        _uiState.update { state ->
                            val idx = state.chats.indexOfFirst { it.id == chatId }
                            if (idx != -1) {
                                val updated = state.chats.toMutableList()
                                val updatedChat = updated[idx].copy(
                                    lastMessage = message,
                                    lastMessageDate = message.timestamp,
                                    unreadCount = if (message.isOutgoing) updated[idx].unreadCount else updated[idx].unreadCount + 1
                                )
                                updated.removeAt(idx)
                                
                                val insertIdx = if (updatedChat.isPinned) {
                                    // Pinned chats maintain their absolute relative order among pinned chats.
                                    // However, without fully sorting by order, we can just put it back at its original position
                                    // or sort by order if available. Since it was pinned, we put it back where it was.
                                    idx
                                } else {
                                    // Unpinned chats move to the top of the unpinned section
                                    val firstUnpinned = updated.indexOfFirst { !it.isPinned }
                                    if (firstUnpinned != -1) firstUnpinned else updated.size
                                }
                                
                                // Insert safely
                                if (insertIdx in 0..updated.size) {
                                    updated.add(insertIdx, updatedChat)
                                } else {
                                    updated.add(updatedChat)
                                }
                                
                                state.copy(chats = updated.filterNot { it.isArchived })
                            } else state
                        }
                        
                        // Fetch custom emoji stickers for the new message
                        fetchCustomEmojiStickers(listOf(message))

                        // If chat is not in the list, fetch it
                        if (_uiState.value.chats.none { it.id == chatId }) {
                            getChat(chatId).onSuccess { chat ->
                                if (chat.isArchived) return@onSuccess
                                _uiState.update { state ->
                                    val newChats = state.chats.toMutableList()
                                    if (newChats.none { it.id == chat.id }) {
                                        val insertIdx = if (chat.isPinned) {
                                            0
                                        } else {
                                            val firstUnpinned = newChats.indexOfFirst { !it.isPinned }
                                            if (firstUnpinned != -1) firstUnpinned else newChats.size
                                        }
                                        newChats.add(insertIdx, chat)
                                    }
                                    state.copy(chats = newChats)
                                }
                                downloadMissingPhotos(listOf(chat))
                                chat.lastMessage?.let { fetchCustomEmojiStickers(listOf(it)) }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun fetchCustomEmojiStickers(messages: List<Message>) {
        val customEmojiIds = messages.flatMap { msg ->
            val blockEmojis = msg.blocks.filterIsInstance<MessageBlock.TextBlock>().flatMap { block ->
                block.content.entities
                    .filter { it.type is DomainText.TextEntityType.CustomEmoji }
                    .map { (it.type as DomainText.TextEntityType.CustomEmoji).customEmojiId }
            }
            val reactionEmojis = msg.reactions.flatMap { reaction ->
                reaction.reactionText.entities
                    .filter { it.type is DomainText.TextEntityType.CustomEmoji }
                    .map { (it.type as DomainText.TextEntityType.CustomEmoji).customEmojiId }
            }
            blockEmojis + reactionEmojis
        }.filter { id -> !_uiState.value.customEmojiStickers.containsKey(id) }.distinct()

        if (customEmojiIds.isEmpty()) return

        viewModelScope.launch {
            getCustomEmojiStickers(customEmojiIds).onSuccess { stickers ->
                _uiState.update { state ->
                    val newStickers = state.customEmojiStickers.toMutableMap()
                    stickers.forEachIndexed { index, sticker ->
                        newStickers[customEmojiIds[index]] = sticker
                    }
                    state.copy(customEmojiStickers = newStickers)
                }
                downloadMissingStickers(stickers)
            }
        }
    }

    private fun downloadMissingStickers(stickers: List<MessageBlock.StickerBlock>) {
        for (sticker in stickers) {
            val fileId = sticker.file.fileId ?: continue
            if (sticker.file.fileUrl != null) continue
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

    fun loadChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, hasMore = true) }
            val result = withTimeoutOrNull(15_000L) { getChats(PAGE_SIZE) }
            if (result == null) {
                _uiState.update { it.copy(loading = false, error = "Loading timed out") }
                return@launch
            }
            result.fold(
                onSuccess = { chats ->
                    _uiState.update {
                        val mainChats = chats.filterNot { chat -> chat.isArchived }
                        it.copy(
                            chats = mainChats,
                            loading = false,
                            hasMore = chats.size >= PAGE_SIZE,
                        )
                    }
                    val mainChats = chats.filterNot { it.isArchived }
                    downloadMissingPhotos(mainChats)
                    fetchCustomEmojiStickers(mainChats.mapNotNull { it.lastMessage })
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, loading = false) }
                }
            )
        }
    }

    /** 滚动到底部时加载更多聊天 */
    fun loadMoreChats() {
        val state = _uiState.value
        if (state.loadingMore || state.loading || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChats(PAGE_SIZE, offset = state.chats.size)
                .fold(
                    onSuccess = { moreChats ->
                        if (moreChats.isEmpty()) {
                            _uiState.update {
                                it.copy(loadingMore = false, hasMore = false)
                            }
                        } else {
                            val mainChats = moreChats.filterNot { chat -> chat.isArchived }
                            val existingIds = state.chats.map { it.id }.toSet()
                            val newChats = mainChats.filter { it.id !in existingIds }
                            _uiState.update {
                                it.copy(
                                    chats = it.chats + newChats,
                                    loadingMore = false,
                                    hasMore = moreChats.size >= PAGE_SIZE,
                                )
                            }
                            downloadMissingPhotos(newChats)
                            fetchCustomEmojiStickers(newChats.mapNotNull { it.lastMessage })
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

    fun refreshChats() {
        viewModelScope.launch {
            val currentChats = _uiState.value.chats
            if (currentChats.isEmpty()) return@launch

            // 重新获取与当前列表相同数量的聊天
            getChats(currentChats.size).onSuccess { freshChats ->
                _uiState.update { state ->
                    val mainChats = freshChats.filterNot { it.isArchived }
                    // 保留已下载的 photoUrl
                    val existingPhotos = state.chats.associate { it.id to it.photoUrl }
                    val merged =
                            mainChats.map { chat ->
                                val existingUrl = existingPhotos[chat.id]
                                if (existingUrl != null && chat.photoUrl == null)
                                        chat.copy(photoUrl = existingUrl)
                                else chat
                            }
                    state.copy(chats = merged)
                }
                val mainChats = freshChats.filterNot { it.isArchived }
                downloadMissingPhotos(mainChats)
                fetchCustomEmojiStickers(mainChats.mapNotNull { it.lastMessage })
            }
        }
    }

    fun togglePin(chatId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            _uiState.update { state ->
                val mutableChats = state.chats.toMutableList()
                val idx = mutableChats.indexOfFirst { it.id == chatId }
                if (idx != -1) {
                    val chat = mutableChats.removeAt(idx).copy(isPinned = isPinned)
                    if (isPinned) {
                        mutableChats.add(0, chat)
                    } else {
                        val firstUnpinned = mutableChats.indexOfFirst { !it.isPinned }
                        val start = if (firstUnpinned != -1) firstUnpinned else mutableChats.size
                        var insertIdx = start
                        while (insertIdx < mutableChats.size && mutableChats[insertIdx].lastMessageDate > chat.lastMessageDate) {
                            insertIdx++
                        }
                        mutableChats.add(insertIdx, chat)
                    }
                    state.copy(chats = mutableChats)
                } else state
            }

            toggleChatPin(chatId, isPinned).onSuccess {
                refreshChats()
            }
        }
    }

    fun toggleArchive(chatId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            toggleChatArchive(chatId, isArchived).onSuccess {
                _uiState.update { state ->
                    if (isArchived) {
                        // Remove from main list when archived
                        state.copy(chats = state.chats.filter { it.id != chatId })
                    } else {
                        val chats = state.chats.map { if (it.id == chatId) it.copy(isArchived = isArchived) else it }
                        state.copy(chats = chats)
                    }
                }
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
