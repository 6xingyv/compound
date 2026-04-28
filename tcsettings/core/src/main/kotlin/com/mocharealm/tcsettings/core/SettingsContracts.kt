package com.mocharealm.tcsettings.core

import kotlinx.coroutines.flow.Flow

interface SettingToken<T>

interface SettingsStore {
    fun <T> flow(token: SettingToken<T>, defaultValue: T): Flow<T>
    suspend fun <T> write(token: SettingToken<T>, value: T)
}

fun interface SettingsInterceptor<T> {
    suspend fun intercept(newValue: T): InterceptorResult
}

sealed interface InterceptorResult {
    data object Success : InterceptorResult
    data class Failure(
        val reason: String? = null,
        val throwable: Throwable? = null
    ) : InterceptorResult
}

class SettingsInterceptorDispatcher(
    private val interceptors: Map<SettingToken<*>, SettingsInterceptor<*>> = emptyMap()
) {
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> dispatch(token: SettingToken<T>, newValue: T): InterceptorResult {
        val interceptor = interceptors[token] ?: return InterceptorResult.Success
        return (interceptor as SettingsInterceptor<T>).intercept(newValue)
    }
}

data class SettingsError(
    val token: SettingToken<*>,
    val message: String?,
    val throwable: Throwable?
)

data class SelectableValue<T>(
    val current: T,
    val options: List<T>
)
