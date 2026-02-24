package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.User

interface PersonNameFormatterRepository {
    fun formatName(user: User, length: NameLength = NameLength.MEDIUM): String
}

enum class NameLength { LONG, MEDIUM, SHORT }