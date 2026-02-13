package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.model.AuthState

class CheckAuthenticationCodeUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(code: String): AuthState {
        return repository.checkAuthenticationCode(code)
    }
}
