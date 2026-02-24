package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository

class FormatPersonNameUseCase(
    private val repository: PersonNameFormatterRepository
) {
    operator fun invoke(user: User) {
        repository.formatName(user)
    }
}