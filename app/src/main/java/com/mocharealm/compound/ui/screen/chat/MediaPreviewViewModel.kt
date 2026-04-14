package com.mocharealm.compound.ui.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.ui.nav.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MediaPreviewState(
    val items: List<MediaItem> = emptyList(),
    val initialIndex: Int = 0
)

class MediaPreviewViewModel(
    private val chatId: Long,
    private val messageId: Long,
    private val getChatMessages: GetChatMessagesUseCase,
    private val downloadFile: DownloadFileUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaPreviewState())
    val uiState: StateFlow<MediaPreviewState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    private fun loadMedia() {
        viewModelScope.launch {
            // Simple implementation: load the specific message and extract media.
            // Ideally this would fetch surrounding media messages too.
            val result = getChatMessages(chatId, 20, messageId, false, -10)
            val messages = result.getOrNull() ?: return@launch

            val items = mutableListOf<MediaItem>()
            var initialIndex = 0

            for (msg in messages) {
                for (block in msg.blocks) {
                    if (block is MessageBlock.MediaBlock) {
                        if (block.file.fileUrl.isNullOrEmpty() && block.file.fileId != null) {
                            viewModelScope.launch {
                                downloadFile(block.file.fileId)
                            }
                        }
                        val item = MediaItem(
                            url = block.file.fileUrl ?: "",
                            thumbnailUrl = block.thumbnail?.fileUrl,
                            type = if (block.mediaType == MessageBlock.MediaBlock.MediaType.VIDEO) MediaItem.MediaType.VIDEO else MediaItem.MediaType.PHOTO,
                            id = block.id
                        )
                        items.add(item)
                        if (msg.id == messageId) {
                            initialIndex = items.size - 1
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                // If getChatMessages didn't return anything or didn't include the target, try just target
                // For simplicity, returning empty if not found in surrounding.
            }

            _uiState.value = MediaPreviewState(items, initialIndex)
        }
    }
}