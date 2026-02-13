package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.model.AuthState

class SetAuthenticationPhoneNumberUseCase(
    private val repository: TelegramRepository
) {
    suspend operator fun invoke(phoneNumber: String): AuthState {
        return repository.setAuthenticationPhoneNumber(phoneNumber)
    }
}
