package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetChatPinnedMessageUseCase(
    private val telegramRepository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long): Result<List<Message>> {
        return telegramRepository.getChatPinnedMessages(chatId)
    }
}