package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.model.AuthState

class GetAuthenticationStateUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(): AuthState {
        return repository.getAuthenticationState()
    }
}
