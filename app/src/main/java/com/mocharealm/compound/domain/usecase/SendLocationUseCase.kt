package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.Message
import com.mocharealm.compound.domain.repository.TelegramRepository

class SendLocationUseCase(private val repository: TelegramRepository) {
    suspend operator fun invoke(
        chatId: Long,
        latitude: Double,
        longitude: Double
    ): Result<Message> = repository.sendLocation(chatId, latitude, longitude)
}
