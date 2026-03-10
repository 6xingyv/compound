package com.mocharealm.compound.domain.model

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val phoneNumber: String? = null,
    val profilePhotoUrl: String? = null
) {
    val name: String
        get() = if (lastName.isEmpty()) firstName else "$firstName $lastName"
    val initials: String
        get() = (firstName.take(1) + lastName.take(1)).uppercase()
}
