package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetChatsUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(limit: Int = 20, offsetChatId: Long = 0): Result<List<Chat>> {
        return repository.getChats(limit, offsetChatId)
    }
}