package com.mocharealm.compound.domain.model

data class Chat(
    val id: Long,
    val title: String,
    val lastMessage: String? = null,
    val lastMessageDate: Long = 0L,
    val unreadCount: Int = 0,
    val isChannel: Boolean = false,
    val isGroup: Boolean = false,
    val photoUrl: String? = null,
    val photoFileId: Int? = null
)