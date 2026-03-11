package com.mocharealm.compound.data.repository

import com.google.firebase.messaging.FirebaseMessaging
import com.mocharealm.compound.data.mapper.UserMapper
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class AuthRepositoryImpl(
    private val tdLibDataSource: TdLibDataSource,
    private val userMapper: UserMapper
) : AuthRepository {

    override suspend fun setAuthenticationPhoneNumber(phoneNumber: String): AuthState {
        return try {
            val response = tdLibDataSource.send(
                TdApi.SetAuthenticationPhoneNumber(
                    phoneNumber,
                    TdApi.PhoneNumberAuthenticationSettings(
                        false, false, true, false, true,
                        TdApi.FirebaseAuthenticationSettingsAndroid(),
                        emptyArray<String>()
                    )
                )
            )
            parseAuthState(response)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun checkAuthenticationCode(code: String): AuthState {
        return try {
            val response = tdLibDataSource.send(TdApi.CheckAuthenticationCode(code))
            parseAuthState(response)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun checkAuthenticationPassword(password: String): AuthState {
        return try {
            val response = tdLibDataSource.send(TdApi.CheckAuthenticationPassword(password))
            parseAuthState(response)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val userObject = tdLibDataSource.send(TdApi.GetMe())
            if (userObject is TdApi.User) {
                Result.success(userMapper.mapUser(userObject))
            } else {
                Result.failure(Exception("Invalid response type"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            tdLibDataSource.send(TdApi.LogOut())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAuthenticationState(): AuthState {
        return try {
            val state = tdLibDataSource.send(TdApi.GetAuthorizationState())
            parseAuthState(state)
        } catch (e: Exception) {
            AuthState.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun awaitAuthState(): AuthState {
        val currentState = getAuthenticationState()
        if (currentState !is AuthState.Error) {
            return currentState
        }

        return tdLibDataSource.authStateFlow
            .map { parseAuthState(it) }
            .filter { it !is AuthState.Error }
            .first()
    }

    override suspend fun registerDevice(token: String): Result<Unit> {
        return try {
            tdLibDataSource.send(
                TdApi.RegisterDevice(
                    TdApi.DeviceTokenFirebaseCloudMessaging(token, false),
                    longArrayOf() // Other user IDs if multiple accounts are used
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun processPushNotification(json: String): Result<Unit> {
        return try {
            val response = tdLibDataSource.send(TdApi.ProcessPushNotification(json))
            if (response is TdApi.Ok) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to process push notification: $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun parseAuthState(state: TdApi.Object): AuthState =
        when (state) {
            is TdApi.Ok -> parseAuthState(tdLibDataSource.send(TdApi.GetAuthorizationState()))
            is TdApi.AuthorizationStateWaitPhoneNumber -> AuthState.WaitingForPhoneNumber
            is TdApi.AuthorizationStateWaitCode -> AuthState.WaitingForOtp
            is TdApi.AuthorizationStateWaitPassword -> AuthState.WaitingForPassword
            is TdApi.AuthorizationStateReady -> {
                val result = runCatching { tdLibDataSource.send(TdApi.GetMe()) }
                    .mapCatching {
                        if (it is TdApi.User) userMapper.mapUser(it) else error("Invalid response")
                    }
                    .fold(
                        onSuccess = { AuthState.Ready(it) },
                        onFailure = { AuthState.Error(it.message ?: "Failed to load user") }
                    )

                if (result is AuthState.Ready) {
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            task.result?.let { token ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    registerDevice(token)
                                }
                            }
                        }
                    }
                }
                result
            }

            else -> AuthState.Error("Unknown authentication state: $state")
        }
}
