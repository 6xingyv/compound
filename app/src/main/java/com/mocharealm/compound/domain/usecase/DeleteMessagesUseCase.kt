package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class DeleteMessagesUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(chatId: Long, messageIds: List<Long>, revoke: Boolean = false): Result<Unit> =
        repository.deleteMessages(chatId, messageIds, revoke)
}
