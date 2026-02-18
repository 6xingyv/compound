package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Chat
import org.drinkless.tdlib.TdApi

data class ChatDto(
    val id: Long,
    val title: String,
    val lastMessage: String? = null,
    val lastMessageDate: Long = 0L,
    val unreadCount: Int = 0,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val photoUrl: String? = null,
    val photoFileId: Int? = null
) {
    fun toDomain(): Chat = Chat(
        id = id,
        title = title,
        lastMessage = lastMessage,
        lastMessageDate = lastMessageDate,
        unreadCount = unreadCount,
        isChannel = isChannel,
        isGroup = isGroup,
        photoUrl = photoUrl,
        photoFileId = photoFileId
    )

    companion object {
        fun fromTdApi(chat: TdApi.Chat): ChatDto {
            val lastMessageText = when (chat.lastMessage?.content) {
                is TdApi.MessageText -> (chat.lastMessage!!.content as TdApi.MessageText).text.text
                is TdApi.MessagePhoto -> "Photo"
                is TdApi.MessageVideo -> "Video"
                is TdApi.MessageDocument -> "Document"
                is TdApi.MessageAudio -> "Audio"
                is TdApi.MessageVoiceNote -> "Voice message"
                else -> "Message $chat"
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
            )
        }
    }
}