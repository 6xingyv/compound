package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class SetChatDraftMessageUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(chatId: Long, replyToMessageId: Long, draftText: String): Result<Unit> {
        return repository.setChatDraftMessage(chatId, replyToMessageId, draftText)
    }
}
