package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class OpenChatUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long): Result<Unit> = repository.openChat(chatId)
}
