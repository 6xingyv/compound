package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.model.TextEntity
import com.mocharealm.compound.domain.repository.TelegramRepository

class SendMessageUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long, text: String, entities: List<TextEntity> = emptyList(), replyToMessageId: Long = 0): Result<Message> =
        repository.sendMessage(chatId, text, entities, replyToMessageId)
}
