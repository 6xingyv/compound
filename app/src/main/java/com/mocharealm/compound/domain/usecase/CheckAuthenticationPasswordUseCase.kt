package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.model.AuthState

class CheckAuthenticationPasswordUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(password: String): AuthState {
        return repository.checkAuthenticationPassword(password)
    }
}
