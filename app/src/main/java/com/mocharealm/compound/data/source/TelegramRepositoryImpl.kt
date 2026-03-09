package com.mocharealm.compound.data.source

import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.repository.AuthRepository
import com.mocharealm.compound.domain.repository.ChatRepository
import com.mocharealm.compound.domain.repository.MediaRepository
import com.mocharealm.compound.domain.repository.MessageRepository
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow

class TelegramRepositoryImpl(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val mediaRepository: MediaRepository
) : TelegramRepository {

    // --- Auth Repository Delegation ---
    override suspend fun setAuthenticationPhoneNumber(phoneNumber: String) = authRepository.setAuthenticationPhoneNumber(phoneNumber)
    override suspend fun checkAuthenticationCode(code: String) = authRepository.checkAuthenticationCode(code)
    override suspend fun checkAuthenticationPassword(password: String) = authRepository.checkAuthenticationPassword(password)
    override suspend fun getCurrentUser() = authRepository.getCurrentUser()
    override suspend fun logout() = authRepository.logout()
    override suspend fun getAuthenticationState() = authRepository.getAuthenticationState()
    override suspend fun awaitAuthState() = authRepository.awaitAuthState()

    // --- Chat Repository Delegation ---
    override suspend fun getChats(limit: Int, offsetChatId: Long) = chatRepository.getChats(limit, offsetChatId)
    override suspend fun getChat(chatId: Long) = chatRepository.getChat(chatId)
    override suspend fun getInternalLink(link: String) = chatRepository.getInternalLink(link)
    override suspend fun openChat(chatId: Long) = chatRepository.openChat(chatId)
    override suspend fun closeChat(chatId: Long) = chatRepository.closeChat(chatId)
    override suspend fun saveChatReadPosition(chatId: Long, messageId: Long) = chatRepository.saveChatReadPosition(chatId, messageId)
    override suspend fun getChatReadPosition(chatId: Long) = chatRepository.getChatReadPosition(chatId)

    // Message Repository Delegation
    override suspend fun getChatMessages(chatId: Long, limit: Int, fromMessageId: Long, onlyLocal: Boolean, offset: Int) =
        messageRepository.getChatMessages(chatId, limit, fromMessageId, onlyLocal, offset)

    override suspend fun sendMessage(chatId: Long, text: String, entities: List<Text.TextEntity>, replyToMessageId: Long) =
        messageRepository.sendMessage(chatId, text, entities, replyToMessageId)

    override suspend fun sendFiles(chatId: Long, files: List<ShareFileInfo>, caption: String, captionEntities: List<Text.TextEntity>, replyToMessageId: Long) =
        messageRepository.sendFiles(chatId, files, caption, captionEntities, replyToMessageId)

    override val messageUpdates: Flow<MessageUpdateEvent> get() = messageRepository.messageUpdates

    override suspend fun sendSticker(chatId: Long, sticker: MessageBlock.StickerBlock) = messageRepository.sendSticker(chatId, sticker)
    override suspend fun sendLocation(chatId: Long, latitude: Double, longitude: Double) = messageRepository.sendLocation(chatId, latitude, longitude)
    override suspend fun setChatDraftMessage(chatId: Long, replyToMessageId: Long, draftText: String) =
        messageRepository.setChatDraftMessage(chatId, replyToMessageId, draftText)

    // Media Repository Delegation
    override suspend fun downloadFile(fileId: Int) = mediaRepository.downloadFile(fileId)
    override fun downloadFileWithProgress(fileId: Int) = mediaRepository.downloadFileWithProgress(fileId)
    override suspend fun getInstalledStickerSets() = mediaRepository.getInstalledStickerSets()
    override suspend fun getStickerSetStickers(setId: Long) = mediaRepository.getStickerSetStickers(setId)
}
