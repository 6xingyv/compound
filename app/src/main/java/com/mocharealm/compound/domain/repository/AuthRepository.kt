package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.model.User

interface AuthRepository {
    suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState
    suspend fun checkAuthenticationCode(code: String): AuthState
    suspend fun checkAuthenticationPassword(password: String): AuthState
    suspend fun getCurrentUser(): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getAuthenticationState(): AuthState
    suspend fun awaitAuthState(): AuthState
    suspend fun registerDevice(token: String): Result<Unit>
    suspend fun processPushNotification(json: String): Result<Unit>
}
