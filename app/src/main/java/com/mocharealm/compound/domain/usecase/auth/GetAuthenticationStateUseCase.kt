package com.mocharealm.compound.domain.usecase.auth

import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.repository.TelegramRepository

class GetAuthenticationStateUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(): AuthState {
        return repository.getAuthenticationState()
    }
}