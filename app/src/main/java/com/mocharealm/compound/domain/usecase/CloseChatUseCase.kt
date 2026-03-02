package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class CloseChatUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long): Result<Unit> = repository.closeChat(chatId)
}
