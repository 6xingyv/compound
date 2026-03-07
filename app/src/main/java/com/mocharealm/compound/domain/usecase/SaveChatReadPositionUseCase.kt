package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class SaveChatReadPositionUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(chatId: Long, messageId: Long) {
        repository.saveChatReadPosition(chatId, messageId)
    }
}
