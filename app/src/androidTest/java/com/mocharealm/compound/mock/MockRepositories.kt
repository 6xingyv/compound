package com.mocharealm.compound.mock

import com.mocharealm.compound.domain.model.*
import com.mocharealm.compound.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MockAuthRepository : AuthRepository {
    override suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState = AuthState.Ready
    override suspend fun checkAuthenticationCode(code: String): AuthState = AuthState.Ready
    override suspend fun checkAuthenticationPassword(password: String): AuthState = AuthState.Ready
    override suspend fun getCurrentUser(): Result<User> = Result.success(User(0, "Test", "User", "testuser", null))
    override suspend fun logout(): Result<Unit> = Result.success(Unit)
    override suspend fun getAuthenticationState(): AuthState = AuthState.Ready
    override suspend fun awaitAuthState(): AuthState = AuthState.Ready
    override suspend fun registerDevice(token: String): Result<Unit> = Result.success(Unit)
    override suspend fun processPushNotification(json: String): Result<Unit> = Result.success(Unit)
}

class MockChatRepository : ChatRepository {
    private val mockChats = listOf(
        Chat(1L, Text("Compound Support"), null, 1678900000, 0, ChatType.DIRECT),
        Chat(2L, Text("Design Team"), null, 1678900001, 5, ChatType.GROUP),
        Chat(3L, Text("Project Updates"), null, 1678900002, 0, ChatType.CHANNEL)
    )

    override suspend fun getChats(limit: Int, offset: Int): Result<List<Chat>> = Result.success(mockChats)
    override suspend fun getChat(chatId: Long): Result<Chat> = Result.success(mockChats.find { it.id == chatId } ?: mockChats[0])
    override suspend fun getInternalLink(link: String): Result<InternalLink> = Result.failure(Exception("Not implemented"))
    override suspend fun openChat(chatId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun closeChat(chatId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun saveChatReadPosition(chatId: Long, messageId: Long) {}
    override suspend fun getChatReadPosition(chatId: Long): Long = 0L
}

class MockMessageRepository : MessageRepository {
    private val _messageUpdates = MutableSharedFlow<MessageUpdateEvent>()
    override val messageUpdates: Flow<MessageUpdateEvent> = _messageUpdates.asSharedFlow()

    override suspend fun getChatMessages(chatId: Long, limit: Int, fromMessageId: Long, onlyLocal: Boolean, offset: Int): Result<List<Message>> {
        val user = User(0, "Test", "User", "testuser", null)
        return Result.success(listOf(
            Message(user, chatId, true, listOf(MessageBlock.TextBlock(1, 1678900000, Text("Hello from Mock!")))),
            Message(user, chatId, false, listOf(MessageBlock.TextBlock(2, 1678900001, Text("This is a mock message list for screenshots.")))),
            Message(user, chatId, true, listOf(MessageBlock.TextBlock(3, 1678900002, Text("Everything looks great!"))))
        ))
    }

    override suspend fun sendMessage(chatId: Long, text: String, entities: List<Text.TextEntity>, replyToMessageId: Long): Result<Message> = Result.failure(Exception("Mock"))
    override suspend fun sendFiles(chatId: Long, files: List<ShareFileInfo>, caption: String, captionEntities: List<Text.TextEntity>, replyToMessageId: Long): Result<List<Message>> = Result.failure(Exception("Mock"))
    override suspend fun sendSticker(chatId: Long, sticker: MessageBlock.StickerBlock): Result<Message> = Result.failure(Exception("Mock"))
    override suspend fun getCustomEmojiStickers(customEmojiIds: List<Long>): Result<List<MessageBlock.StickerBlock>> = Result.success(emptyList())
    override suspend fun sendLocation(chatId: Long, latitude: Double, longitude: Double): Result<Message> = Result.failure(Exception("Mock"))
    override suspend fun setChatDraftMessage(chatId: Long, replyToMessageId: Long, draftText: String): Result<Unit> = Result.success(Unit)
}
