package com.mocharealm.compound.data.dto

import com.mocharealm.compound.domain.model.User
import org.drinkless.tdlib.TdApi

data class UserDto(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val phoneNumber: String,
    val profilePhotoUrl: String? = null
) {
    fun toDomain(): User = User(
        id = id,
        firstName = firstName,
        lastName = lastName,
        username = username,
        phoneNumber = phoneNumber,
        profilePhotoUrl = profilePhotoUrl
    )

    companion object {
        fun fromTdApi(user: TdApi.User, photoPath: String?): UserDto {
            return UserDto(
                id = user.id,
                firstName = user.firstName,
                lastName = user.lastName,
                username = user.usernames?.activeUsernames?.lastOrNull() ?: "",
                phoneNumber = user.phoneNumber,
                profilePhotoUrl = photoPath
            )
        }
    }
}
