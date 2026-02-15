package com.mocharealm.compound.data.source

import android.content.Context
import android.os.Build
import com.mocharealm.compound.BuildConfig
import com.mocharealm.compound.data.dto.ChatDto
import com.mocharealm.compound.data.dto.MessageDto
import com.mocharealm.compound.data.dto.UserDto
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageType
import com.mocharealm.compound.domain.model.ReplyInfo
import com.mocharealm.compound.domain.model.StickerFormat
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.model.TextEntityType
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TelegramRepositoryImpl(
    private val client: Client,
    private val context: Context,
    private val fileUpdates: SharedFlow<TdApi.UpdateFile>,
    private val updates: SharedFlow<TdApi.Update>
) : TelegramRepository {
    private suspend fun <T : TdApi.Object> send(query: TdApi.Function<T>): T =
        suspendCancellableCoroutine { cont ->
            client.send(
                query,
                { result: TdApi.Object? ->
                    when (result) {
                        is TdApi.Error -> cont.resumeWithException(Exception(result.message))
                        null -> cont.resumeWithException(NullPointerException("Result is null"))
                        else -> {
                            @Suppress("UNCHECKED_CAST")
                            cont.resume(result as T)
                        }
                    }
                },
                { e: Throwable? ->
                    cont.resumeWithException(e ?: Exception("Unknown error"))
                }
            )
        }

    private suspend fun <T : TdApi.Object> sendSafe(query: TdApi.Function<T>): Result<T> =
        runCatching { send(query) }

    init {
        runBlocking {
            send(
                TdApi.SetTdlibParameters(
                    false,
                    context.filesDir.absolutePath,
                    context.filesDir.absolutePath,
                    ByteArray(0),
                    true,
                    true,
                    true,
                    true,
                    BuildConfig.TD_API_ID,
                    BuildConfig.TD_API_HASH,
                    Locale.getDefault().toString(),
                    Build.MODEL,
                    Build.VERSION.RELEASE,
                    BuildConfig.VERSION_NAME
                )
            )
        }
    }

    override suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState {
        return try {
            val response = send(
                TdApi.SetAuthenticationPhoneNumber(
                    phoneNumber,
                    TdApi.PhoneNumberAuthenticationSettings(
                        false,
                        false,
                        true,
                        false,
                        true,
                        TdApi.FirebaseAuthenticationSettingsAndroid(),
                        emptyArray<String>()
                    )
                )
            )
            parseAuthState(response)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun checkAuthenticationCode(code: String): AuthState {
        return try {
            val response = send(TdApi.CheckAuthenticationCode(code))
            parseAuthState(response)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun checkAuthenticationPassword(password: String): AuthState {
        return try {
            val response = send(TdApi.CheckAuthenticationPassword(password))
            parseAuthState(response)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val userObject = send(TdApi.GetMe())
            if (userObject is TdApi.User) {
                Result.success(mapUser(userObject))
            } else {
                Result.failure(Exception("Invalid response type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            send(TdApi.LogOut())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuthenticationState(): AuthState {
        return try {
            val state = send(TdApi.GetAuthorizationState())
            parseAuthState(state)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getChats(limit: Int, offsetChatId: Long): Result<List<Chat>> = runCatching {
        // 持续调用 LoadChats 把足够数量的聊天加载进 TDLib 内部列表
        // offsetChatId != 0 时说明已经加载过前面的，需要多加载一些
        val totalNeeded = if (offsetChatId == 0L) limit else {
            // 需要获取 offset 之后的 limit 条，先把前面的 + 后面的都加载进来
            limit + limit // 多加载一些以确保有足够的后续数据
        }
        // 循环调用 LoadChats 直到加载够或者没有更多
        var loaded = 0
        while (loaded < totalNeeded) {
            val batch = runCatching { send(TdApi.LoadChats(TdApi.ChatListMain(), totalNeeded - loaded)) }
            if (batch.isFailure) break // 404 = 没有更多聊天了
            loaded += totalNeeded - loaded
        }

        val chatList = send(TdApi.GetChats(TdApi.ChatListMain(), totalNeeded))
        if (chatList !is TdApi.Chats) error("Invalid chat list response")

        val chatIds = if (offsetChatId == 0L) {
            chatList.chatIds.take(limit)
        } else {
            val idx = chatList.chatIds.indexOf(offsetChatId)
            if (idx >= 0 && idx + 1 < chatList.chatIds.size) {
                chatList.chatIds.drop(idx + 1).take(limit)
            } else {
                emptyList()
            }
        }

        val chats = mutableListOf<Chat>()
        for (id in chatIds) {
            val chat = runCatching { send(TdApi.GetChat(id)) }.getOrNull()
            if (chat is TdApi.Chat) {
                chats.add(mapChatFast(chat))
            }
        }
        chats
    }

    override suspend fun getChatMessages(chatId: Long, limit: Int, fromMessageId: Long, onlyLocal: Boolean, offset: Int): Result<List<Message>> = runCatching {
        val result = send(
            TdApi.GetChatHistory(chatId, fromMessageId, offset, limit, onlyLocal)
        )
        if (result !is TdApi.Messages) error("Invalid messages response type")

        val messages = mutableListOf<Message>()
        for (msg in result.messages) {
            messages.add(mapMessageFast(msg))
        }
        messages.reversed()
    }

    // ── Mapping helpers (fast, no network) ─────────────────────────────

    private suspend fun parseAuthState(state: TdApi.Object): AuthState = when (state) {
        is TdApi.Ok -> parseAuthState(send(TdApi.GetAuthorizationState()))
        is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.WaitingForPhoneNumber
        is TdApi.AuthorizationStateWaitCode -> AuthState.WaitingForOtp
        is TdApi.AuthorizationStateWaitPassword -> AuthState.WaitingForPassword
        is TdApi.AuthorizationStateReady -> {
            runCatching { send(TdApi.GetMe()) }
                .mapCatching { if (it is TdApi.User) mapUser(it) else error("Invalid response") }
                .fold(
                    onSuccess = { AuthState.Ready(it) },
                    onFailure = { AuthState.Error(it.message ?: "Failed to load user") }
                )
        }
        else -> AuthState.Error("Unknown authentication state: $state")
    }

    // ── File download ────────────────────────────────────────────────────

    override suspend fun sendMessage(
        chatId: Long,
        text: String,
        entities: List<TextEntity>,
        replyToMessageId: Long
    ): Result<Message> {
        return try {
            val (finalText, finalEntities) = if (entities.isEmpty()) {
                com.mocharealm.compound.domain.util.MarkdownParser.parseForSending(text)
            } else {
                text to entities
            }

            val content = TdApi.InputMessageText(
                TdApi.FormattedText(
                    finalText,
                    mapToTdApiEntities(finalEntities)
                ),
                null,
                true
            )

            val replyTo = if (replyToMessageId != 0L) {
                TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0)
            } else {
                null
            }

            val sentMessage = send(
                TdApi.SendMessage(
                    chatId,
                    null,
                    replyTo,
                    null,
                    null,
                    content
                )
            )
            Result.success(mapMessageFast(sentMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun downloadFile(fileId: Int): Result<String> = runCatching {
        // First check if file is already downloaded locally
        val existingFile = send(TdApi.GetFile(fileId))
        if (existingFile.local?.isDownloadingCompleted == true) {
            return@runCatching existingFile.local.path
        }
        // Download synchronously — TDLib returns the completed file directly
        val downloaded = send(TdApi.DownloadFile(fileId, 32, 0, 0, true))
        downloaded.local?.path ?: error("Download failed: local path is null")
    }

    override suspend fun getChat(chatId: Long): Result<Chat> = runCatching {
        val chat = send(TdApi.GetChat(chatId))
        if (chat !is TdApi.Chat) error("Invalid chat response")
        mapChatFast(chat)
    }

    override val messageUpdates: Flow<MessageUpdateEvent> = updates
        .transform { update ->
            when (update) {
                is TdApi.UpdateNewMessage -> emit(MessageUpdateEvent.NewMessage(mapMessageFast(update.message)))
                is TdApi.UpdateMessageContent -> {
                    val msg = sendSafe(TdApi.GetMessage(update.chatId, update.messageId)).getOrNull()
                    if (msg is TdApi.Message) emit(MessageUpdateEvent.MessageUpdated(mapMessageFast(msg)))
                }
                is TdApi.UpdateMessageEdited -> {
                    val msg = sendSafe(TdApi.GetMessage(update.chatId, update.messageId)).getOrNull()
                    if (msg is TdApi.Message) emit(MessageUpdateEvent.MessageUpdated(mapMessageFast(msg)))
                }
                is TdApi.UpdateMessageSendSucceeded -> {
                    val msg = sendSafe(TdApi.GetMessage(update.message.chatId, update.message.id)).getOrNull()
                    if (msg is TdApi.Message) emit(MessageUpdateEvent.MessageSendSucceeded(update.oldMessageId, mapMessageFast(msg)))
                }
            }
        }

    private suspend fun getLocalFileOrDownload(file: TdApi.File): TdApi.LocalFile? {
        if (file.local?.isDownloadingCompleted == true) return file.local

        val downloaded = sendSafe(TdApi.DownloadFile(file.id, 32, 0, 0, true)).getOrNull()
        if (downloaded?.local?.isDownloadingCompleted == true) return downloaded.local

        // Fallback: wait for file update event
        return withTimeoutOrNull(10_000L) {
            fileUpdates
                .filter { it.file.id == file.id }
                .map { it.file.local }
                .first { it.isDownloadingCompleted }
        }
    }

    // ── Object mappers ───────────────────────────────────────────────────

    private suspend fun mapUser(user: TdApi.User): User {
        val photoPath = user.profilePhoto?.small?.let { getLocalFileOrDownload(it)?.path }
        return UserDto(
            id = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            username = user.usernames?.activeUsernames?.lastOrNull() ?: "",
            phoneNumber = user.phoneNumber,
            profilePhotoUrl = photoPath
        ).toDomain()
    }

    /** Fast mapper — uses already-downloaded local path only, never blocks on download. */
    private fun mapChatFast(chat: TdApi.Chat): Chat {
        val lastMessageText = when (chat.lastMessage?.content) {
            is TdApi.MessageText -> (chat.lastMessage!!.content as TdApi.MessageText).text.text
            is TdApi.MessagePhoto -> "Photo"
            is TdApi.MessageVideo -> "Video"
            is TdApi.MessageDocument -> "Document"
            is TdApi.MessageAudio -> "Audio"
            is TdApi.MessageVoiceNote -> "Voice message"
            else -> "Message"
        }
        val small = chat.photo?.small
        val localPath = small?.local?.takeIf { it.isDownloadingCompleted }?.path
        return ChatDto(
            id = chat.id,
            title = chat.title,
            lastMessage = lastMessageText,
            lastMessageDate = chat.lastMessage?.date?.toLong() ?: 0L,
            unreadCount = chat.unreadCount,
            isChannel = chat.type is TdApi.ChatTypeSupergroup &&
                    (chat.type as TdApi.ChatTypeSupergroup).isChannel,
            isGroup = chat.type is TdApi.ChatTypeBasicGroup ||
                    (chat.type is TdApi.ChatTypeSupergroup &&
                            !(chat.type as TdApi.ChatTypeSupergroup).isChannel),
            photoUrl = localPath,
            photoFileId = small?.id
        ).toDomain()
    }

    /** Suspend mapper — resolves real sender name and avatar via TDLib. */
    private suspend fun mapMessageFast(msg: TdApi.Message): Message {
        val parsed = parseMessageContent(msg.content)

        val senderId: Long
        val senderName: String
        val avatarPath: String?

        when (msg.senderId) {
            is TdApi.MessageSenderUser -> {
                val userId = (msg.senderId as TdApi.MessageSenderUser).userId
                senderId = userId
                val user = sendSafe(TdApi.GetUser(userId)).getOrNull()
                senderName = if (user != null) {
                    buildString {
                        append(user.firstName)
                        if (user.lastName.isNotEmpty()) {
                            append(' ').append(user.lastName)
                        }
                    }
                } else {
                    "User $userId"
                }
                val smallPhoto = user?.profilePhoto?.small
                avatarPath = smallPhoto?.local?.takeIf { it.isDownloadingCompleted }?.path
                    ?: smallPhoto?.let { getLocalFileOrDownload(it)?.path }
            }
            is TdApi.MessageSenderChat -> {
                val chatId = (msg.senderId as TdApi.MessageSenderChat).chatId
                senderId = chatId
                val chat = sendSafe(TdApi.GetChat(chatId)).getOrNull()
                senderName = chat?.title ?: "Chat $chatId"
                val smallPhoto = chat?.photo?.small
                avatarPath = smallPhoto?.local?.takeIf { it.isDownloadingCompleted }?.path
                    ?: smallPhoto?.let { getLocalFileOrDownload(it)?.path }
            }
            else -> {
                senderId = 0L
                senderName = "Unknown"
                avatarPath = null
            }
        }

        // ── Reply info ──────────────────────────────────────────────────
        val replyInfo = resolveReplyInfo(msg)

        return MessageDto(
            id = msg.id,
            chatId = msg.chatId,
            senderId = senderId,
            senderName = senderName,
            content = parsed.text,
            timestamp = msg.date.toLong(),
            isOutgoing = msg.isOutgoing,
            messageType = parsed.type,
            fileId = parsed.fileId,
            avatarUrl = avatarPath,
            stickerFormat = parsed.stickerFormat,
            entities = parsed.entities,
            replyTo = replyInfo,
            mediaAlbumId = msg.mediaAlbumId,
            hasSpoiler = parsed.hasSpoiler
        ).toDomain()
    }

    private suspend fun resolveReplyInfo(msg: TdApi.Message): ReplyInfo? {
        val reply = msg.replyTo as? TdApi.MessageReplyToMessage ?: return null
        val replyChatId = if (reply.chatId != 0L) reply.chatId else msg.chatId
        val replyMsgId = reply.messageId
        if (replyMsgId == 0L) return null

        // Try to get the original message for sender info
        val originalMsg = sendSafe(TdApi.GetMessage(replyChatId, replyMsgId)).getOrNull()

        val replySenderName = if (originalMsg != null) {
            when (originalMsg.senderId) {
                is TdApi.MessageSenderUser -> {
                    val u = sendSafe(TdApi.GetUser((originalMsg.senderId as TdApi.MessageSenderUser).userId)).getOrNull()
                    u?.let {
                        buildString {
                            append(it.firstName)
                            if (it.lastName.isNotEmpty()) append(' ').append(it.lastName)
                        }
                    } ?: "User"
                }
                is TdApi.MessageSenderChat -> {
                    val c = sendSafe(TdApi.GetChat((originalMsg.senderId as TdApi.MessageSenderChat).chatId)).getOrNull()
                    c?.title ?: "Chat"
                }
                else -> "Unknown"
            }
        } else {
            // If message is from another chat, try origin
            when (reply.origin) {
                is TdApi.MessageOriginUser -> {
                    val u = sendSafe(TdApi.GetUser((reply.origin as TdApi.MessageOriginUser).senderUserId)).getOrNull()
                    u?.let {
                        buildString {
                            append(it.firstName)
                            if (it.lastName.isNotEmpty()) append(' ').append(it.lastName)
                        }
                    } ?: "User"
                }
                is TdApi.MessageOriginHiddenUser -> (reply.origin as TdApi.MessageOriginHiddenUser).senderName
                is TdApi.MessageOriginChat -> {
                    val c = sendSafe(TdApi.GetChat((reply.origin as TdApi.MessageOriginChat).senderChatId)).getOrNull()
                    c?.title ?: "Chat"
                }
                is TdApi.MessageOriginChannel -> {
                    val c = sendSafe(TdApi.GetChat((reply.origin as TdApi.MessageOriginChannel).chatId)).getOrNull()
                    c?.title ?: "Channel"
                }
                else -> "Unknown"
            }
        }

        // Preview text: prefer quote text, then original message content summary
        val previewText = reply.quote?.text?.text
            ?: if (originalMsg != null) {
                val p = parseMessageContent(originalMsg.content)
                p.text
            } else {
                reply.content?.let { parseMessageContent(it).text } ?: ""
            }

        return ReplyInfo(
            messageId = replyMsgId,
            senderName = replySenderName,
            text = previewText
        )
    }

    private data class ParsedContent(
        val text: String,
        val type: MessageType,
        val fileId: Int?,
        val stickerFormat: StickerFormat? = null,
        val entities: List<TextEntity> = emptyList(),
        val hasSpoiler: Boolean = false
    )

    private fun parseMessageContent(content: TdApi.MessageContent): ParsedContent =
        when (content) {
            is TdApi.MessageText -> ParsedContent(
                content.text.text,
                MessageType.TEXT,
                null,
                entities = mapFormattedTextEntities(content.text)
            )
            is TdApi.MessagePhoto -> {
                val caption = content.caption.text
                val photoFileId = content.photo.sizes.lastOrNull()?.photo?.id
                ParsedContent(
                    if (caption.isNotEmpty()) "Photo: $caption" else "Photo",
                    MessageType.PHOTO,
                    photoFileId,
                    entities = if (caption.isNotEmpty()) mapFormattedTextEntities(content.caption) else emptyList(),
                    hasSpoiler = content.hasSpoiler
                )
            }
            is TdApi.MessageSticker -> {
                val format = when (content.sticker.format) {
                    is TdApi.StickerFormatWebp -> StickerFormat.WEBP
                    is TdApi.StickerFormatTgs -> StickerFormat.TGS
                    is TdApi.StickerFormatWebm -> StickerFormat.WEBM
                    else -> StickerFormat.WEBP
                }
                ParsedContent("Sticker", MessageType.STICKER, content.sticker.sticker.id, format)
            }
            is TdApi.MessageVideo -> {
                val caption = content.caption.text
                ParsedContent(
                    if (caption.isNotEmpty()) "Video: $caption" else "Video", 
                    MessageType.VIDEO, 
                    null,
                    hasSpoiler = content.hasSpoiler
                )
            }
            is TdApi.MessageDocument -> ParsedContent("Document: ${content.document.fileName}", MessageType.DOCUMENT, null)
            is TdApi.MessageAudio -> ParsedContent("Audio", MessageType.AUDIO, null)
            is TdApi.MessageVoiceNote -> ParsedContent("Voice message", MessageType.VOICE, null)
            else -> ParsedContent("Message", MessageType.TEXT, null)
        }

    private fun mapFormattedTextEntities(formattedText: TdApi.FormattedText): List<TextEntity> {
        if (formattedText.entities.isNullOrEmpty()) return emptyList()
        return formattedText.entities.mapNotNull { entity ->
            val type = when (entity.type) {
                is TdApi.TextEntityTypeBold -> TextEntityType.Bold
                is TdApi.TextEntityTypeItalic -> TextEntityType.Italic
                is TdApi.TextEntityTypeUnderline -> TextEntityType.Underline
                is TdApi.TextEntityTypeStrikethrough -> TextEntityType.Strikethrough
                is TdApi.TextEntityTypeCode -> TextEntityType.Code
                is TdApi.TextEntityTypePre -> TextEntityType.Pre
                is TdApi.TextEntityTypePreCode -> TextEntityType.PreCode(
                    (entity.type as TdApi.TextEntityTypePreCode).language
                )
                is TdApi.TextEntityTypeTextUrl -> TextEntityType.TextUrl(
                    (entity.type as TdApi.TextEntityTypeTextUrl).url
                )
                is TdApi.TextEntityTypeUrl -> TextEntityType.Url
                is TdApi.TextEntityTypeMention -> TextEntityType.Mention
                is TdApi.TextEntityTypeMentionName -> TextEntityType.Mention
                is TdApi.TextEntityTypeSpoiler -> TextEntityType.Spoiler
                is TdApi.TextEntityTypeEmailAddress -> TextEntityType.EmailAddress
                is TdApi.TextEntityTypePhoneNumber -> TextEntityType.PhoneNumber
                else -> null
            } ?: return@mapNotNull null
            TextEntity(offset = entity.offset, length = entity.length, type = type)
            TextEntity(offset = entity.offset, length = entity.length, type = type)
        }
    }

    private fun mapToTdApiEntities(entities: List<TextEntity>): Array<TdApi.TextEntity> {
        return entities.map { entity ->
            val type = when (entity.type) {
                is TextEntityType.Bold -> TdApi.TextEntityTypeBold()
                is TextEntityType.Italic -> TdApi.TextEntityTypeItalic()
                is TextEntityType.Underline -> TdApi.TextEntityTypeUnderline()
                is TextEntityType.Strikethrough -> TdApi.TextEntityTypeStrikethrough()
                is TextEntityType.Code -> TdApi.TextEntityTypeCode()
                is TextEntityType.Pre -> TdApi.TextEntityTypePre()
                is TextEntityType.PreCode -> TdApi.TextEntityTypePreCode(entity.type.language)
                is TextEntityType.TextUrl -> TdApi.TextEntityTypeTextUrl(entity.type.url)
                is TextEntityType.Url -> TdApi.TextEntityTypeUrl()
                is TextEntityType.Mention -> TdApi.TextEntityTypeMention()
                is TextEntityType.Spoiler -> TdApi.TextEntityTypeSpoiler()
                is TextEntityType.EmailAddress -> TdApi.TextEntityTypeEmailAddress()
                is TextEntityType.PhoneNumber -> TdApi.TextEntityTypePhoneNumber()
            }
            TdApi.TextEntity(entity.offset, entity.length, type)
        }.toTypedArray()
    }
}
