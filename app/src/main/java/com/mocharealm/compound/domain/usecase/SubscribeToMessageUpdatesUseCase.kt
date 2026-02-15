package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.MessageUpdateEvent
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.flow.Flow

class SubscribeToMessageUpdatesUseCase(
    private val repository: TelegramRepository
) {
    operator fun invoke(): Flow<MessageUpdateEvent> = repository.messageUpdates
}
