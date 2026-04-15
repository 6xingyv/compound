package com.mocharealm.tcsettings.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.reflect.KClass

interface SettingToken<T>

data class SelectableValue<T>(
    val current: T,
    val options: List<T>
)

interface SettingsOptionsProvider<T> {
    val options: Flow<List<T>>
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
    private val interceptors: Map<KClass<*>, SettingsInterceptor<Any?>> = emptyMap()
) {
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> dispatch(tokenClass: KClass<*>, newValue: T): InterceptorResult {
        val interceptor = interceptors[tokenClass] ?: return InterceptorResult.Success
        return (interceptor as SettingsInterceptor<T>).intercept(newValue)
    }
}

interface SettingsStore {
    fun <T> flow(tokenClass: KClass<*>, defaultValue: T): Flow<T>
    suspend fun <T> write(tokenClass: KClass<*>, value: T)
}

class InMemorySettingsStore : SettingsStore {
    private val values = MutableStateFlow<Map<KClass<*>, Any?>>(emptyMap())

    @Suppress("UNCHECKED_CAST")
    override fun <T> flow(tokenClass: KClass<*>, defaultValue: T): Flow<T> {
        return values.map { map ->
            (map[tokenClass] as T?) ?: defaultValue
        }
    }

    override suspend fun <T> write(tokenClass: KClass<*>, value: T) {
        values.value = values.value + (tokenClass to value)
    }
}
