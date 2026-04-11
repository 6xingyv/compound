package com.mocharealm.compound.ui.screen.msglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.Text as DomainText
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.GetCustomEmojiStickersUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import com.mocharealm.compound.domain.usecase.ToggleChatArchiveUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ArchivedMsgListViewModel(
    private val getChats: GetChatsUseCase,
    private val downloadFile: DownloadFileUseCase,
    private val subscribeToMessageUpdates: SubscribeToMessageUpdatesUseCase,
    private val getChat: GetChatUseCase,
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
                                updated[idx] = updatedChat
                                state.copy(chats = updated)
                            } else state
                        }

                        fetchCustomEmojiStickers(listOf(message))

                        if (_uiState.value.chats.none { it.id == chatId }) {
                            getChat(chatId).onSuccess { chat ->
                                if (!chat.isArchived) return@onSuccess
                                _uiState.update { state ->
                                    if (state.chats.any { it.id == chat.id }) return@update state
                                    state.copy(chats = listOf(chat) + state.chats)
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
            msg.blocks.filterIsInstance<MessageBlock.TextBlock>().flatMap { block ->
                block.content.entities
                    .filter { it.type is DomainText.TextEntityType.CustomEmoji }
                    .map { (it.type as DomainText.TextEntityType.CustomEmoji).customEmojiId }
            }
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
            val result = withTimeoutOrNull(15_000L) { getChats(PAGE_SIZE, archived = true) }
            if (result == null) {
                _uiState.update { it.copy(loading = false, error = "Loading timed out") }
                return@launch
            }
            result.fold(
                onSuccess = { chats ->
                    val archivedChats = chats.filter { it.isArchived }
                    _uiState.update {
                        it.copy(
                            chats = archivedChats,
                            loading = false,
                            hasMore = chats.size >= PAGE_SIZE,
                        )
                    }
                    downloadMissingPhotos(archivedChats)
                    fetchCustomEmojiStickers(archivedChats.mapNotNull { it.lastMessage })
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, loading = false) }
                }
            )
        }
    }

    fun loadMoreChats() {
        val state = _uiState.value
        if (state.loadingMore || state.loading || !state.hasMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            getChats(PAGE_SIZE, offset = state.chats.size, archived = true)
                .fold(
                    onSuccess = { moreChats ->
                        if (moreChats.isEmpty()) {
                            _uiState.update {
                                it.copy(loadingMore = false, hasMore = false)
                            }
                        } else {
                            val archivedChats = moreChats.filter { chat -> chat.isArchived }
                            val existingIds = state.chats.map { it.id }.toSet()
                            val newChats = archivedChats.filter { it.id !in existingIds }
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

    fun toggleArchive(chatId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            toggleChatArchive(chatId, isArchived).onSuccess {
                _uiState.update { state ->
                    if (!isArchived) {
                        state.copy(chats = state.chats.filter { it.id != chatId })
                    } else {
                        state
                    }
                }
            }
        }
    }
}
