package com.mocharealm.compound.ui.nav

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.auth.AwaitAuthenticationStateUseCase
import com.mocharealm.compound.ui.util.ShareIntentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppNavViewModel(
    private val deepLinkHandler: DeepLinkHandler,
    private val awaitAuthState: AwaitAuthenticationStateUseCase,
) : ViewModel() {

    private val _initialBackstack = MutableStateFlow<List<Screen>>(listOf(Screen.Intro))
    val initialBackstack: StateFlow<List<Screen>> = _initialBackstack

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    /**
     * Called once from MainActivity.onCreate with the launch intent.
     * Resolves auth state, deep link, and share payloads, then signals splash to dismiss.
     */
    fun initialize(intent: Intent, context: Context) {
        viewModelScope.launch {
            val authState = awaitAuthState()
            val isLoggedIn = authState is AuthState.Ready

            var deepLinkScreen: Screen? = null
            var sharePickerScreen: Screen? = null

            if (isLoggedIn) {
                // If it's a SHARE intent, parse local files in IO dispatcher
                if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
                    val payload = withContext(Dispatchers.IO) {
                        ShareIntentParser.extractPayload(context, intent)
                    }
                    if (payload != null) sharePickerScreen = Screen.SharePicker(payload)
                } 
                // Alternatively, if it's a deep link (VIEW or just has intent data)
                else if (intent.data != null) {
                    deepLinkScreen = deepLinkHandler.resolve(intent.data!!)
                }
            }

            _initialBackstack.value = when {
                !isLoggedIn -> listOf(Screen.Intro)
                sharePickerScreen != null -> listOf(Screen.Home, sharePickerScreen)
                deepLinkScreen != null -> listOf(Screen.Home, deepLinkScreen)
                else -> listOf(Screen.Home)
            }

            _isReady.value = true
        }
    }
}
