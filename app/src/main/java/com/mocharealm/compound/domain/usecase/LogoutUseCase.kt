package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository

class LogoutUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.logout()
    }
}
