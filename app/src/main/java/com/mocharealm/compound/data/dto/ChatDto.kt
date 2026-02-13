package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.Chat

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
}