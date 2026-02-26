package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.ChatType
import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.User
import org.drinkless.tdlib.TdApi

data class ChatDto(
        val id: Long,
        val title: String,
        val lastMessage: Message? = null,
        val lastMessageDate: Long = 0L,
        val unreadCount: Int = 0,
        val isChannel: Boolean = false,
        val isGroup: Boolean = false,
        val photoUrl: String? = null,
        val photoFileId: Int? = null,
) {
    fun toDomain(): Chat =
            Chat(
                    id = id,
                    title = title,
                    lastMessage = lastMessage,
                    lastMessageDate = lastMessageDate,
                    unreadCount = unreadCount,
                    type =
                            if (isGroup) ChatType.GROUP
                            else if (isChannel) ChatType.CHANNEL else ChatType.DIRECT,
                    photoUrl = photoUrl,
                    photoFileId = photoFileId
            )

    companion object {
        fun fromTdApi(chat: TdApi.Chat): ChatDto {
            val small = chat.photo?.small
            val localPath = small?.local?.takeIf { it.isDownloadingCompleted }?.path

            val type = chat.type
            val supergroup = type as? TdApi.ChatTypeSupergroup
            val isChannel = supergroup?.isChannel == true
            val isGroup =
                    type is TdApi.ChatTypeBasicGroup ||
                            (supergroup != null && !supergroup.isChannel)

            val lastMsg = chat.lastMessage
            val lastMessage =
                    lastMsg?.let { msg ->
                        val senderId =
                                when (val s = msg.senderId) {
                                    is TdApi.MessageSenderUser -> s.userId
                                    is TdApi.MessageSenderChat -> s.chatId
                                    else -> 0L
                                }
                        val blocks =
                                MessageDto.parseMessageContent(
                                        content = msg.content,
                                        messageId = msg.id,
                                        timestamp = msg.date.toLong(),
                                )
                        Message(
                                sender =
                                        User(
                                                id = senderId,
                                                firstName = "",
                                                lastName = "",
                                                username = "",
                                        ),
                                chatId = chat.id,
                                isOutgoing = msg.isOutgoing,
                                blocks = blocks,
                        )
                    }

            return ChatDto(
                    id = chat.id,
                    title = chat.title,
                    lastMessage = lastMessage,
                    lastMessageDate = chat.lastMessage?.date?.toLong() ?: 0L,
                    unreadCount = chat.unreadCount,
                    isChannel = isChannel,
                    isGroup = isGroup,
                    photoUrl = localPath,
                    photoFileId = small?.id
            )
        }
    }
}
