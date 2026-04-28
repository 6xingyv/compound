package com.mocharealm.tcsettings.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mocharealm.tcsettings.core.InterceptorResult
import com.mocharealm.tcsettings.core.SettingToken
import com.mocharealm.tcsettings.core.SettingsError
import com.mocharealm.tcsettings.core.SettingsInterceptorDispatcher
import com.mocharealm.tcsettings.core.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class SettingsController(
    private val settingsStore: SettingsStore,
    private val dispatcher: SettingsInterceptorDispatcher,
    private val scope: CoroutineScope
) {
    private val _errors = MutableSharedFlow<SettingsError>()
    val errors: SharedFlow<SettingsError> = _errors

    fun <T> update(token: SettingToken<T>, value: T, onResult: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = dispatcher.dispatch(token, value)
            when (result) {
                is InterceptorResult.Success -> {
                    settingsStore.write(token, value)
                    onResult?.invoke(true)
                }
                is InterceptorResult.Failure -> {
                    _errors.emit(SettingsError(token, result.reason, result.throwable))
                    onResult?.invoke(false)
                }
            }
        }
    }
}

@Composable
fun rememberSettingsController(
    settingsStore: SettingsStore = koinInject(),
    dispatcher: SettingsInterceptorDispatcher = koinInject()
): SettingsController {
    val scope = rememberCoroutineScope()
    return remember(settingsStore, dispatcher) {
        SettingsController(settingsStore, dispatcher, scope)
    }
}
