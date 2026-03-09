package com.mocharealm.compound.data.repository

import com.mocharealm.compound.data.mapper.ChatMapper
import com.mocharealm.compound.data.mapper.MessageMapper
import com.mocharealm.compound.data.source.local.ChatLocalDataSource
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.InternalLink
import com.mocharealm.compound.domain.repository.ChatRepository
import org.drinkless.tdlib.TdApi

class ChatRepositoryImpl(
    private val tdLibDataSource: TdLibDataSource,
    private val localDataSource: ChatLocalDataSource,
    private val chatMapper: ChatMapper,
    private val messageMapper: MessageMapper
) : ChatRepository {

    override suspend fun getChats(limit: Int, offsetChatId: Long): Result<List<Chat>> =
        runCatching {
            // Continuously call LoadChats to load enough chats into TDLib internal list.
            // When offsetChatId != 0, it means we've already loaded some, so we need more.
            val totalNeeded = if (offsetChatId == 0L) limit else limit + limit
            var loaded = 0
            while (loaded < totalNeeded) {
                val batch = runCatching {
                    tdLibDataSource.send(TdApi.LoadChats(TdApi.ChatListMain(), totalNeeded - loaded))
                }
                if (batch.isFailure) break // 404 = no more chats
                loaded += totalNeeded - loaded
            }

            val chatList = tdLibDataSource.send(TdApi.GetChats(TdApi.ChatListMain(), totalNeeded))
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
                val tdChat = runCatching { tdLibDataSource.send(TdApi.GetChat(id)) }.getOrNull() ?: continue
                chats.add(chatMapper.mapChat(tdChat))
            }
            chats
        }

    override suspend fun getChat(chatId: Long): Result<Chat> = runCatching {
        val chat = tdLibDataSource.send(TdApi.GetChat(chatId))
        if (chat !is TdApi.Chat) error("Invalid chat response")
        chatMapper.mapChat(chat)
    }

    override suspend fun getInternalLink(link: String): Result<InternalLink> = runCatching {
        val linkType = tdLibDataSource.send(TdApi.GetInternalLinkType(link))
        toDomainInternalLink(linkType)
    }

    override suspend fun openChat(chatId: Long): Result<Unit> = runCatching {
        tdLibDataSource.send(TdApi.OpenChat(chatId))
        Result.success(Unit)
    }

    override suspend fun closeChat(chatId: Long): Result<Unit> = runCatching {
        tdLibDataSource.send(TdApi.CloseChat(chatId))
        Result.success(Unit)
    }

    override suspend fun saveChatReadPosition(chatId: Long, messageId: Long) {
        localDataSource.saveReadPosition(chatId, messageId)
    }

    override suspend fun getChatReadPosition(chatId: Long): Long {
        return localDataSource.getReadPosition(chatId)
    }

    private suspend fun toDomainInternalLink(type: TdApi.InternalLinkType): InternalLink =
        when (type) {
            is TdApi.InternalLinkTypeChatInvite -> {
                val info = tdLibDataSource.send(TdApi.CheckChatInviteLink(type.inviteLink))
                val small = info.photo?.small
                val localPath = small?.local?.takeIf { it.isDownloadingCompleted }?.path
                InternalLink.ChatInvite(
                    Chat(
                        id = info.chatId,
                        title = info.title,
                        lastMessage = null,
                        unreadCount = 0,
                        type = when (info.type) {
                            is TdApi.InviteLinkChatTypeBasicGroup,
                            is TdApi.InviteLinkChatTypeSupergroup -> ChatType.GROUP
                            is TdApi.InviteLinkChatTypeChannel -> ChatType.CHANNEL
                            else -> ChatType.GROUP
                        },
                        photoUrl = localPath,
                        photoFileId = small?.id
                    )
                )
            }
            is TdApi.InternalLinkTypeMessage -> {
                val msgLink = tdLibDataSource.send(TdApi.GetMessageLinkInfo(type.url))
                val chatId = msgLink.chatId
                val chat = getChat(chatId).getOrThrow()
                val msg = msgLink.message?.let { messageMapper.mapSingleTdMessage(it) }
                if (msg != null) {
                    InternalLink.Message(chat, msg)
                } else {
                    InternalLink.ChatInvite(chat)
                }
            }
            else -> InternalLink.Generic(type.toString())
        }
}
