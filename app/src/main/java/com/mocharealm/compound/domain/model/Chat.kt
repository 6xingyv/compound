package com.mocharealm.compound.domain.model

data class Chat(
    val id: Long,
    val title: Text,
    val lastMessage: Message? = null,
    val lastMessageDate: Long = 0L,
    val unreadCount: Int = 0,
    val unreadMentionCount: Int = 0,
    val unreadReactionCount: Int = 0,
    val type: ChatType = ChatType.DIRECT,
    val photoUrl: String? = null,
    val photoFileId: Int? = null,
    val draftMessage: String? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val order: Long = 0L
)

enum class ChatType {
    DIRECT,
    GROUP,
    CHANNEL
}
