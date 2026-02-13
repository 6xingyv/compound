package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.model.User

class GetCurrentUserUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(): Result<User> {
        return repository.getCurrentUser()
    }
}
