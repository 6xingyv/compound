package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetChatMessagesUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, limit: Int = 20, fromMessageId: Long = 0, onlyLocal: Boolean = false): Result<List<Message>> {
        return repository.getChatMessages(chatId, limit, fromMessageId, onlyLocal)
    }
}