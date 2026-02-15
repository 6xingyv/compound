package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Chat
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetChatUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(chatId: Long): Result<Chat> = repository.getChat(chatId)
}
