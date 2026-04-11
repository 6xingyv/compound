package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.model.InternalLink

interface ChatRepository {
    suspend fun getChats(limit: Int = 20, offset: Int = 0, archived: Boolean = false): Result<List<Chat>>
    suspend fun getChat(chatId: Long): Result<Chat>
    suspend fun getInternalLink(link: String): Result<InternalLink>
    suspend fun openChat(chatId: Long): Result<Unit>
    suspend fun closeChat(chatId: Long): Result<Unit>
    suspend fun saveChatReadPosition(chatId: Long, messageId: Long)
    suspend fun getChatReadPosition(chatId: Long): Long
}
