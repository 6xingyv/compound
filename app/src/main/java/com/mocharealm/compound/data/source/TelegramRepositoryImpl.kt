package com.mocharealm.compound.data.source

import android.content.Context
import android.os.Build
import com.mocharealm.compound.BuildConfig
import com.mocharealm.compound.data.dto.ChatDto
import com.mocharealm.compound.data.dto.MessageDto
import com.mocharealm.compound.data.dto.UserDto
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.DownloadProgress
import com.mocharealm.compound.domain.model.File
import com.mocharealm.compound.domain.model.InternalLink
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.MessageBlock
import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.model.ShareFileInfo
import com.mocharealm.compound.domain.model.StickerSetInfo
import com.mocharealm.compound.domain.model.Text
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.util.MarkdownParser
import com.mocharealm.compound.domain.util.ShareProtocol
import com.mocharealm.tci18n.core.tdLangPackId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
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
    private val updates: SharedFlow<TdApi.Update>
) : TelegramRepository {
    private suspend fun <T : TdApi.Object> send(query: TdApi.Function<T>): T =
        suspendCancellableCoroutine { cont ->
            client.send(
                query,
                { result: TdApi.Object? ->
                    when (result) {
                        is TdApi.Error ->
                            cont.resumeWithException(Exception(result.message))

                        null ->
                            cont.resumeWithException(
                                NullPointerException("Result is null")
                            )

                        else -> {
                            @Suppress("UNCHECKED_CAST") cont.resume(result as T)
                        }
                    }
                },
                { e: Throwable? ->
                    cont.resumeWithException(e ?: Exception("Unknown error"))
                }
            )
        }

    private suspend fun <T : TdApi.Object> sendSafe(query: TdApi.Function<T>): Result<T> =
        runCatching {
            send(query)
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            updates.filterIsInstance<TdApi.UpdateAuthorizationState>().collect { update ->
                if (update.authorizationState is TdApi.AuthorizationStateWaitTdlibParameters) {
                    val langPackId = tdLangPackId(Locale.getDefault())
                    val dbDir = java.io.File(context.filesDir, "i18n")
                    val db = java.io.File(dbDir, "$langPackId.sqlite")
                    if (!dbDir.exists()) {
                        dbDir.mkdirs()
                    }

                    // MUST use raw client.send to bypass coroutine suspension.
                    // TDLib may not immediately resolve these option requests until SetTdlibParameters is handled.
                    client.send(
                        TdApi.SetOption(
                            "language_pack_database_path",
                            TdApi.OptionValueString(db.absolutePath)
                        )
                    ) { }

                    client.send(
                        TdApi.SetOption(
                            "localization_target",
                            TdApi.OptionValueString("android")
                        )
                    ) { }

                    client.send(
                        TdApi.SetOption(
                            "language_pack_id",
                            TdApi.OptionValueString(langPackId)
                        )
                    ) { }

                    sendSafe(
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
        }
    }

    override suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState {
        return try {
            val response =
                send(
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

    override suspend fun awaitAuthState(): AuthState {
        // 先查一次当前状态
        val currentState = getAuthenticationState()
        // 如果是一个有效状态（不是 Error，说明 TDLib 参数等已就绪），就直接返回
        if (currentState !is AuthState.Error) {
            return currentState
        }

        // 如果是中间状态/Error，则等待 TDLib 的正式通知
        return updates
            .filterIsInstance<TdApi.UpdateAuthorizationState>()
            .map { parseAuthState(it.authorizationState) }
            .filter { it !is AuthState.Error }
            .first()
    }

    override suspend fun getInternalLink(link: String): Result<InternalLink> = runCatching {
        send(TdApi.GetInternalLinkType(link)).toDomain()
    }

    override suspend fun openChat(chatId: Long): Result<Unit> = runCatching {
        send(TdApi.OpenChat(chatId))
        Unit
    }

    override suspend fun closeChat(chatId: Long): Result<Unit> = runCatching {
        send(TdApi.CloseChat(chatId))
        Unit
    }

    private suspend fun TdApi.InternalLinkType.toDomain(): InternalLink =
        when (this) {
            is TdApi.InternalLinkTypeChatInvite -> {
                val info = send(TdApi.CheckChatInviteLink(inviteLink))
                val small = info.photo?.small
                val localPath = small?.local?.takeIf { it.isDownloadingCompleted }?.path
                InternalLink.ChatInvite(
                    Chat(
                        id = info.chatId,
                        title = info.title,
                        lastMessage = null,
                        unreadCount = 0,
                        type =
                            when (info.type) {
                                is TdApi.InviteLinkChatTypeBasicGroup,
                                is TdApi.InviteLinkChatTypeSupergroup ->
                                    ChatType.GROUP

                                is TdApi.InviteLinkChatTypeChannel ->
                                    ChatType.CHANNEL

                                else -> ChatType.GROUP
                            },
                        photoUrl = localPath,
                        photoFileId = small?.id
                    )
                )
            }

            is TdApi.InternalLinkTypeMessage -> {
                // Open the link via TDLib to resolve the actual message
                val msgLink = send(TdApi.GetMessageLinkInfo(url))
                val chatId = msgLink.chatId
                val chat = getChat(chatId).getOrThrow()
                val msg = msgLink.message?.let { mapSingleTdMessage(it) }
                if (msg != null) {
                    InternalLink.Message(chat, msg)
                } else {
                    // Message not accessible, fall back to chat
                    InternalLink.ChatInvite(chat)
                }
            }

            else -> InternalLink.Generic(this.toString())
        }

    override suspend fun getChats(limit: Int, offsetChatId: Long): Result<List<Chat>> =
        runCatching {
            // 持续调用 LoadChats 把足够数量的聊天加载进 TDLib 内部列表
            // offsetChatId != 0 时说明已经加载过前面的，需要多加载一些
            val totalNeeded =
                if (offsetChatId == 0L) limit
                else {
                    // 需要获取 offset 之后的 limit 条，先把前面的 + 后面的都加载进来
                    limit + limit // 多加载一些以确保有足够的后续数据
                }
            // 循环调用 LoadChats 直到加载够或者没有更多
            var loaded = 0
            while (loaded < totalNeeded) {
                val batch = runCatching {
                    send(TdApi.LoadChats(TdApi.ChatListMain(), totalNeeded - loaded))
                }
                if (batch.isFailure) break // 404 = 没有更多聊天了
                loaded += totalNeeded - loaded
            }

            val chatList = send(TdApi.GetChats(TdApi.ChatListMain(), totalNeeded))
            if (chatList !is TdApi.Chats) error("Invalid chat list response")

            val chatIds =
                if (offsetChatId == 0L) {
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
                val tdChat = runCatching { send(TdApi.GetChat(id)) }.getOrNull() ?: continue
                chats.add(ChatDto.fromTdApi(tdChat).toDomain())
            }
            chats
        }

    override suspend fun getChatMessages(
        chatId: Long,
        limit: Int,
        fromMessageId: Long,
        onlyLocal: Boolean,
        offset: Int
    ): Result<List<Message>> = runCatching {
        val result = send(TdApi.GetChatHistory(chatId, fromMessageId, offset, limit, onlyLocal))
        if (result !is TdApi.Messages) error("Invalid messages response type")

        val rawMessages = result.messages.map { mapSingleTdMessage(it) }
        aggregateAlbums(rawMessages)
    }

    private suspend fun parseAuthState(state: TdApi.Object): AuthState =
        when (state) {
            is TdApi.Ok -> parseAuthState(send(TdApi.GetAuthorizationState()))
            is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.WaitingForPhoneNumber
            is TdApi.AuthorizationStateWaitCode -> AuthState.WaitingForOtp
            is TdApi.AuthorizationStateWaitPassword -> AuthState.WaitingForPassword
            is TdApi.AuthorizationStateReady -> {
                runCatching { send(TdApi.GetMe()) }
                    .mapCatching {
                        if (it is TdApi.User) mapUser(it) else error("Invalid response")
                    }
                    .fold(
                        onSuccess = { AuthState.Ready(it) },
                        onFailure = {
                            AuthState.Error(it.message ?: "Failed to load user")
                        }
                    )
            }

            else -> AuthState.Error("Unknown authentication state: $state")
        }

    override suspend fun sendMessage(
        chatId: Long,
        text: String,
        entities: List<Text.TextEntity>,
        replyToMessageId: Long
    ): Result<Message> {
        return try {
            val (finalText, finalEntities) =
                if (entities.isEmpty()) {
                    MarkdownParser.parseForSending(text)
                } else {
                    text to entities
                }

            val content =
                TdApi.InputMessageText(
                    TdApi.FormattedText(
                        finalText,
                        MessageDto.mapToTdApiEntities(finalEntities)
                    ),
                    null,
                    true
                )

            val replyTo =
                if (replyToMessageId != 0L) {
                    TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0)
                } else {
                    null
                }

            val sentMessage = send(TdApi.SendMessage(chatId, null, replyTo, null, null, content))
            Result.success(mapSingleTdMessage(sentMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendFiles(
        chatId: Long,
        files: List<ShareFileInfo>,
        caption: String,
        captionEntities: List<Text.TextEntity>,
        replyToMessageId: Long
    ): Result<List<Message>> = runCatching {
        val formattedCaption =
            TdApi.FormattedText(caption, MessageDto.mapToTdApiEntities(captionEntities))

        val contents =
            files.mapIndexed { index, file ->
                val isLast = index == files.lastIndex
                val cap =
                    if (isLast) formattedCaption else TdApi.FormattedText("", emptyArray())

                val thumbnail =
                    file.thumbnailPath?.let { path ->
                        TdApi.InputThumbnail(TdApi.InputFileLocal(path), 320, 320)
                    }

                TdApi.InputMessageDocument(
                    TdApi.InputFileLocal(file.filePath),
                    thumbnail,
                    false,
                    cap
                )
            }

        val replyTo =
            if (replyToMessageId != 0L) {
                TdApi.InputMessageReplyToMessage(replyToMessageId, null, 0)
            } else null

        if (contents.size == 1) {
            val msg = send(TdApi.SendMessage(chatId, null, replyTo, null, null, contents[0]))
            listOf(mapSingleTdMessage(msg))
        } else {
            val messages =
                send(
                    TdApi.SendMessageAlbum(
                        chatId,
                        null,
                        replyTo,
                        null,
                        contents.toTypedArray()
                    )
                )
            val rawMessages = messages.messages.map { mapSingleTdMessage(it) }
            aggregateAlbums(rawMessages)
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

    override fun downloadFileWithProgress(fileId: Int): Flow<DownloadProgress> = channelFlow {
        // Check if already downloaded
        val existingFile = this@TelegramRepositoryImpl.send(TdApi.GetFile(fileId))
        if (existingFile.local?.isDownloadingCompleted == true) {
            send(DownloadProgress.Completed(existingFile.local.path))
            return@channelFlow
        }

        // Start async download (synchronous = false)
        this@TelegramRepositoryImpl.sendSafe(TdApi.DownloadFile(fileId, 32, 0, 0, false))

        // Collect progress from UpdateFile events
        updates.filter { it is TdApi.UpdateFile && it.file.id == fileId }.collect { update ->
            val file = (update as TdApi.UpdateFile).file
            if (file.local.isDownloadingCompleted) {
                send(DownloadProgress.Completed(file.local.path))
                return@collect
            } else if (file.expectedSize > 0) {
                val percent = (file.local.downloadedSize * 100 / file.expectedSize).toInt()
                send(DownloadProgress.Downloading(percent))
            }
        }
    }

    override suspend fun getChat(chatId: Long): Result<Chat> = runCatching {
        val chat = send(TdApi.GetChat(chatId))
        if (chat !is TdApi.Chat) error("Invalid chat response")
        ChatDto.fromTdApi(chat).toDomain()
    }

    override val messageUpdates: Flow<MessageUpdateEvent> =
        updates.transform { update ->
            when (update) {
                is TdApi.UpdateNewMessage ->
                    emit(MessageUpdateEvent.NewMessage(mapSingleTdMessage(update.message)))

                is TdApi.UpdateMessageContent -> {
                    val msg =
                        sendSafe(TdApi.GetMessage(update.chatId, update.messageId))
                            .getOrNull()
                    if (msg is TdApi.Message)
                        emit(MessageUpdateEvent.MessageUpdated(mapSingleTdMessage(msg)))
                }

                is TdApi.UpdateMessageEdited -> {
                    val msg =
                        sendSafe(TdApi.GetMessage(update.chatId, update.messageId))
                            .getOrNull()
                    if (msg is TdApi.Message)
                        emit(MessageUpdateEvent.MessageUpdated(mapSingleTdMessage(msg)))
                }

                is TdApi.UpdateMessageSendSucceeded -> {
                    val msg =
                        sendSafe(TdApi.GetMessage(update.message.chatId, update.message.id))
                            .getOrNull()
                    if (msg is TdApi.Message)
                        emit(
                            MessageUpdateEvent.MessageSendSucceeded(
                                update.oldMessageId,
                                mapSingleTdMessage(msg)
                            )
                        )
                }
            }
        }

    private suspend fun getLocalFileOrDownload(file: TdApi.File): TdApi.LocalFile? {
        if (file.local?.isDownloadingCompleted == true) return file.local

        val downloaded = sendSafe(TdApi.DownloadFile(file.id, 32, 0, 0, true)).getOrNull()
        if (downloaded?.local?.isDownloadingCompleted == true) return downloaded.local

        // Fallback: wait for file update event
        return withTimeoutOrNull(10_000L) {
            updates
                .filter { it is TdApi.UpdateFile && it.file.id == file.id }
                .map { (it as TdApi.UpdateFile).file.local }
                .first { it.isDownloadingCompleted }
        }
    }

    private suspend fun mapUser(user: TdApi.User): User {
        val photoPath = user.profilePhoto?.small?.let { getLocalFileOrDownload(it)?.path }
        return UserDto.fromTdApi(user, photoPath).toDomain()
    }

    /**
     * Maps a single TdApi.Message to a domain Message with a single block. Album aggregation
     * happens in [aggregateAlbums].
     */
    private suspend fun mapSingleTdMessage(msg: TdApi.Message): Message {
        val sender = resolveSender(msg)
        val replyTo = resolveReplyMessage(msg)

        // System messages get a typed SystemActionBlock
        val blocks: List<MessageBlock> =
            resolveSystemBlock(msg, sender)?.let { listOf(it) }
                ?: MessageDto.parseMessageContent(
                    content = msg.content,
                    messageId = msg.id,
                    timestamp = msg.date.toLong(),
                    mediaAlbumId = msg.mediaAlbumId,
                )

        // ShareInfo decoding for text blocks
        val textBlock = blocks.filterIsInstance<MessageBlock.TextBlock>().lastOrNull()
        val shareInfo =
            if (textBlock != null) {
                ShareProtocol.decode(textBlock.content.content, textBlock.content.entities)
            } else null

        // Strip share protocol from text content if present
        val finalBlocks =
            if (shareInfo != null && textBlock != null) {
                val (strippedContent, strippedEntities) =
                    ShareProtocol.strip(
                        textBlock.content.content,
                        textBlock.content.entities
                    )
                blocks.map { b ->
                    if (b === textBlock)
                        b.copy(content = Text(strippedContent, strippedEntities))
                    else b
                }
            } else blocks

        return Message(
            sender = sender,
            chatId = msg.chatId,
            isOutgoing = msg.isOutgoing,
            blocks = finalBlocks,
            replyTo = replyTo,
            shareInfo = shareInfo,
        )
    }

    /**
     * Aggregates messages with matching mediaAlbumId into a single Message with multiple blocks.
     * Non-album messages pass through unchanged.
     */
    private fun aggregateAlbums(messages: List<Message>): List<Message> {
        val result = mutableListOf<Message>()
        var i = 0
        while (i < messages.size) {
            val msg = messages[i]
            val albumId =
                when (val firstBlock = msg.blocks.firstOrNull()) {
                    is MessageBlock.MediaBlock -> firstBlock.mediaAlbumId
                    is MessageBlock.DocumentBlock -> firstBlock.mediaAlbumId
                    else -> 0L
                }

            if (albumId != 0L) {
                val albumBlocks = mutableListOf<MessageBlock>()
                albumBlocks.addAll(msg.blocks)
                var j = i + 1
                while (j < messages.size) {
                    val nextMsg = messages[j]
                    val nextAlbumId =
                        when (val nextFirstBlock = nextMsg.blocks.firstOrNull()) {
                            is MessageBlock.MediaBlock -> nextFirstBlock.mediaAlbumId
                            is MessageBlock.DocumentBlock -> nextFirstBlock.mediaAlbumId
                            else -> 0L
                        }
                    if (nextAlbumId == albumId) {
                        albumBlocks.addAll(nextMsg.blocks)
                        j++
                    } else break
                }
                // Sort to ensure TextBlock(s) are at the end of the album.
                albumBlocks.sortBy { it is MessageBlock.TextBlock }
                result.add(msg.copy(blocks = albumBlocks))
                i = j
            } else {
                result.add(msg.copy(blocks = msg.blocks.sortedBy { it is MessageBlock.TextBlock }))
                i++
            }
        }
        return result
    }

    private suspend fun resolveSender(msg: TdApi.Message): User {
        return when (msg.senderId) {
            is TdApi.MessageSenderUser -> {
                val userId = (msg.senderId as TdApi.MessageSenderUser).userId
                val user = sendSafe(TdApi.GetUser(userId)).getOrNull()
                if (user != null) {
                    val smallPhoto = user.profilePhoto?.small
                    val avatarPath =
                        smallPhoto?.local?.takeIf { it.isDownloadingCompleted }?.path
                            ?: smallPhoto?.let { getLocalFileOrDownload(it)?.path }
                    User(
                        id = userId,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        username = user.usernames?.activeUsernames?.firstOrNull() ?: "",
                        profilePhotoUrl = avatarPath,
                    )
                } else {
                    User(id = userId, firstName = "User $userId", lastName = "", username = "")
                }
            }

            is TdApi.MessageSenderChat -> {
                val chatId = (msg.senderId as TdApi.MessageSenderChat).chatId
                val chat = sendSafe(TdApi.GetChat(chatId)).getOrNull()
                val smallPhoto = chat?.photo?.small
                val avatarPath =
                    smallPhoto?.local?.takeIf { it.isDownloadingCompleted }?.path
                        ?: smallPhoto?.let { getLocalFileOrDownload(it)?.path }
                User(
                    id = chatId,
                    firstName = chat?.title ?: "Chat $chatId",
                    lastName = "",
                    username = "",
                    profilePhotoUrl = avatarPath,
                )
            }

            else -> User(id = 0L, firstName = "Unknown", lastName = "", username = "")
        }
    }

    private suspend fun resolveSystemBlock(
        msg: TdApi.Message,
        sender: User,
    ): MessageBlock.SystemActionBlock? {
        val actionType: MessageBlock.SystemActionBlock.SystemActionType =
            when (val c = msg.content) {
                is TdApi.MessageChatAddMembers -> {
                    val addedNames =
                        c.memberUserIds.map { userId ->
                            val u = sendSafe(TdApi.GetUser(userId)).getOrNull()
                            u?.let { "${it.firstName} ${it.lastName}".trim() } ?: "$userId"
                        }
                    val addedIds = c.memberUserIds.toList()
                    if (addedIds.size == 1 && addedIds[0] == sender.id) {
                        MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink(
                            userId = sender.id,
                            userName = "${sender.firstName} ${sender.lastName}".trim()
                        )
                    } else {
                        MessageBlock.SystemActionBlock.SystemActionType.MemberJoined(
                            actorId = sender.id,
                            actorName = "${sender.firstName} ${sender.lastName}".trim(),
                            targetId = addedIds.firstOrNull() ?: 0L,
                            targetName = addedNames.joinToString(", ")
                        )
                    }
                }

                is TdApi.MessageChatJoinByLink -> {
                    MessageBlock.SystemActionBlock.SystemActionType.MemberJoinedByLink(
                        userId = sender.id,
                        userName = "${sender.firstName} ${sender.lastName}".trim()
                    )
                }

                is TdApi.MessageChatChangeTitle -> {
                    MessageBlock.SystemActionBlock.SystemActionType.ChatChangedTitle(
                        actorName = "${sender.firstName} ${sender.lastName}".trim(),
                        newTitle = c.title
                    )
                }

                is TdApi.MessagePinMessage -> {
                    MessageBlock.SystemActionBlock.SystemActionType.PinMessage(
                        actorName = "${sender.firstName} ${sender.lastName}".trim(),
                        messagePreview = c.messageId.toString()
                    )
                }

                // Other system types that we don't have typed variants for yet
                is TdApi.MessageChatDeleteMember,
                is TdApi.MessageChatChangePhoto,
                is TdApi.MessageChatUpgradeTo -> return null // fall through to generic parsing
                else -> return null
            }

        return MessageBlock.SystemActionBlock(
            id = msg.id,
            timestamp = msg.date.toLong(),
            type = actionType
        )
    }

    private suspend fun resolveReplyMessage(msg: TdApi.Message): Message? {
        val reply = msg.replyTo as? TdApi.MessageReplyToMessage ?: return null
        val replyChatId = if (reply.chatId != 0L) reply.chatId else msg.chatId
        val replyMsgId = reply.messageId
        if (replyMsgId == 0L) return null

        val originalMsg = sendSafe(TdApi.GetMessage(replyChatId, replyMsgId)).getOrNull()

        val replySender =
            if (originalMsg != null) {
                resolveSender(originalMsg)
            } else {
                // If message is from another chat, try origin
                val name =
                    when (reply.origin) {
                        is TdApi.MessageOriginUser -> {
                            val u =
                                sendSafe(
                                    TdApi.GetUser(
                                        (reply.origin as
                                                TdApi.MessageOriginUser)
                                            .senderUserId
                                    )
                                )
                                    .getOrNull()
                            u?.let { "${it.firstName} ${it.lastName}".trim() } ?: "User"
                        }

                        is TdApi.MessageOriginHiddenUser ->
                            (reply.origin as TdApi.MessageOriginHiddenUser).senderName

                        is TdApi.MessageOriginChat -> {
                            val c =
                                sendSafe(
                                    TdApi.GetChat(
                                        (reply.origin as
                                                TdApi.MessageOriginChat)
                                            .senderChatId
                                    )
                                )
                                    .getOrNull()
                            c?.title ?: "Chat"
                        }

                        is TdApi.MessageOriginChannel -> {
                            val c =
                                sendSafe(
                                    TdApi.GetChat(
                                        (reply.origin as
                                                TdApi.MessageOriginChannel)
                                            .chatId
                                    )
                                )
                                    .getOrNull()
                            c?.title ?: "Channel"
                        }

                        else -> "Unknown"
                    }
                User(id = 0L, firstName = name, lastName = "", username = "")
            }

        val previewText =
            reply.quote?.text?.text
                ?: if (originalMsg != null) {
                    val blocks =
                        MessageDto.parseMessageContent(
                            originalMsg.content,
                            replyMsgId,
                            0
                        )
                    when (val first = blocks.first()) {
                        is MessageBlock.TextBlock -> first.content.content
                        is MessageBlock.MediaBlock -> {
                            val caption =
                                blocks.filterIsInstance<MessageBlock.TextBlock>()
                                    .firstOrNull()
                            caption?.content?.content ?: "Photo"
                        }

                        is MessageBlock.StickerBlock -> "Sticker"
                        is MessageBlock.DocumentBlock -> first.document.fileName
                        is MessageBlock.SystemActionBlock -> "System message"
                        is MessageBlock.VenueBlock -> first.venue.name
                    }
                } else {
                    reply.content?.let {
                        val blocks = MessageDto.parseMessageContent(it, 0, 0)
                        when (val first = blocks.first()) {
                            is MessageBlock.TextBlock -> first.content.content
                            else -> ""
                        }
                    }
                        ?: ""
                }

        val replyBlock =
            MessageBlock.TextBlock(
                id = replyMsgId,
                timestamp = originalMsg?.date?.toLong() ?: 0L,
                content = Text(previewText),
            )

        return Message(
            sender = replySender,
            chatId = replyChatId,
            isOutgoing = originalMsg?.isOutgoing ?: false,
            blocks = listOf(replyBlock),
        )
    }

    override suspend fun getInstalledStickerSets(): Result<List<StickerSetInfo>> =
        runCatching {
            val result = send(
                TdApi.GetInstalledStickerSets(TdApi.StickerTypeRegular())
            )
            val stickerSets = result as TdApi.StickerSets
            stickerSets.sets.map { setInfo ->
                StickerSetInfo(
                    id = setInfo.id,
                    title = setInfo.title,
                    name = setInfo.name,
                    thumbnailFileId = setInfo.thumbnail?.file?.id,
                )
            }
        }

    override suspend fun getStickerSetStickers(setId: Long): Result<List<MessageBlock.StickerBlock>> =
        runCatching {
            val result = send(TdApi.GetStickerSet(setId))
            val stickerSet = result as TdApi.StickerSet
            stickerSet.stickers.map { sticker ->
                val format = MessageDto.mapStickerFormat(sticker.format)
                MessageBlock.StickerBlock(
                    id = sticker.id,
                    timestamp = 0L,
                    stickerFormat = format,
                    file = File(
                        fileId = sticker.sticker.id,
                        fileUrl = sticker.sticker.local?.takeIf { it.isDownloadingCompleted }?.path,
                    ),
                    thumbnail = sticker.thumbnail?.let {
                        File(
                            fileId = it.file.id,
                            fileUrl = it.file.local?.takeIf { l -> l.isDownloadingCompleted }?.path
                        )
                    },
                    caption = Text(sticker.emoji),
                )
            }
        }

    override suspend fun sendSticker(
        chatId: Long,
        sticker: MessageBlock.StickerBlock
    ): Result<Message> = runCatching {
        val fileId = sticker.file.fileId ?: error("Sticker has no fileId")
        val content = TdApi.InputMessageSticker(
            TdApi.InputFileId(fileId),
            null,
            0, 0,
            sticker.caption.content
        )
        val msg = send(TdApi.SendMessage(chatId, null, null, null, null, content))
        mapSingleTdMessage(msg)
    }

    override suspend fun sendLocation(
        chatId: Long,
        latitude: Double,
        longitude: Double
    ): Result<Message> = runCatching {
        val location = TdApi.Location(latitude, longitude, 0.0)
        val content = TdApi.InputMessageLocation(location, 0, 0, 0)
        val msg = send(TdApi.SendMessage(chatId, null, null, null, null, content))
        mapSingleTdMessage(msg)
    }

    override suspend fun setChatDraftMessage(
        chatId: Long,
        replyToMessageId: Long,
        draftText: String
    ): Result<Unit> = runCatching {
        val draft = if (draftText.isEmpty()) null else TdApi.DraftMessage().apply {
            this.replyTo = if (replyToMessageId != 0L) TdApi.InputMessageReplyToMessage(
                replyToMessageId,
                null,
                0
            ) else null
            this.inputMessageText =
                TdApi.InputMessageText(TdApi.FormattedText(draftText, emptyArray()), null, true)
        }
        send(
            TdApi.SetChatDraftMessage(
                chatId,
                null,
                draft,
            )
        )
    }

    override suspend fun saveChatReadPosition(chatId: Long, messageId: Long) {
        val prefs = context.getSharedPreferences("chat_read_positions", Context.MODE_PRIVATE)
        prefs.edit().putLong("chat_$chatId", messageId).apply()
    }

    override suspend fun getChatReadPosition(chatId: Long): Long {
        val prefs = context.getSharedPreferences("chat_read_positions", Context.MODE_PRIVATE)
        return prefs.getLong("chat_$chatId", 0L)
    }
}

