package com.mocharealm.compound.ui.screen.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.CheckAuthenticationCodeUseCase
import com.mocharealm.compound.domain.usecase.CheckAuthenticationPasswordUseCase
import com.mocharealm.compound.domain.usecase.GetAuthenticationStateUseCase
import com.mocharealm.compound.domain.usecase.SetAuthenticationPhoneNumberUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.input.TextFieldState

data class SignInUiState(
    val authState: AuthState = AuthState.WaitingForPhoneNumber,
    val loading: Boolean = false,
    val error: String? = null
)

class SignInViewModel(
    private val setAuthenticationPhoneNumber: SetAuthenticationPhoneNumberUseCase,
    private val checkAuthenticationCode: CheckAuthenticationCodeUseCase,
    private val checkAuthenticationPassword: CheckAuthenticationPasswordUseCase,
    private val getAuthenticationState: GetAuthenticationStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState

    val phone = TextFieldState()
    val code = TextFieldState()
    val password = TextFieldState()

    init {
        refreshAuthState()
    }

    fun submitPhone() {
        val phoneText = phone.text
        if (phoneText.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = setAuthenticationPhoneNumber(phoneText as String)
            _uiState.update {
                it.copy(authState = result, loading = false).withErrorIfNeeded(result)
            }
        }
    }

    fun submitCode() {
        val codeText = code.text
        if (codeText.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = checkAuthenticationCode(codeText as String)
            _uiState.update {
                it.copy(authState = result, loading = false).withErrorIfNeeded(result)
            }
        }
    }

    fun submitPassword() {
        val passwordText = password.text
        if (passwordText.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = checkAuthenticationPassword(passwordText as String)
            _uiState.update {
                it.copy(authState = result, loading = false).withErrorIfNeeded(result)
            }
        }
    }

    fun refreshAuthState() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = getAuthenticationState()
            _uiState.update {
                it.copy(authState = result, loading = false).withErrorIfNeeded(result)
            }
        }
    }

    private fun SignInUiState.withErrorIfNeeded(result: AuthState): SignInUiState {
        return if (result is AuthState.Error) copy(error = result.message) else this
    }
}