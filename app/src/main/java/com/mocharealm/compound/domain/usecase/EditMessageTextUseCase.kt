package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.repository.TelegramRepository

class EditMessageTextUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(chatId: Long, messageId: Long, text: String): Result<Message> =
        repository.editMessageText(chatId, messageId, text)
}
