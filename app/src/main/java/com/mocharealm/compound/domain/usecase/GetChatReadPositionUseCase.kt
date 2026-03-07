package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class GetChatReadPositionUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(chatId: Long): Long {
        return repository.getChatReadPosition(chatId)
    }
}
