package com.mocharealm.compound.domain.model

data class Chat(
    val id: Long,
    val title: String,
    val lastMessage: String? = null,
    val lastMessageDate: Long = 0L,
    val unreadCount: Int = 0,
    val type: ChatType = ChatType.DIRECT,
    val photoUrl: String? = null,
    val photoFileId: Int? = null
)

enum class ChatType{
    DIRECT,
    GROUP,
    CHANNEL
}