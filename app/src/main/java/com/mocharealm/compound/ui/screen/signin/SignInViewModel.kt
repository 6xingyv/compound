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

data class SignInUiState(
    val phone: String = "",
    val code: String = "",
    val password: String = "",
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

    init {
        refreshAuthState()
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value, error = null) }
    }

    fun onCodeChange(value: String) {
        _uiState.update { it.copy(code = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun submitPhone() {
        val phone = _uiState.value.phone
        if (phone.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = setAuthenticationPhoneNumber(phone)
            _uiState.update {
                it.copy(authState = result, loading = false).withErrorIfNeeded(result)
            }
        }
    }

    fun submitCode() {
        val code = _uiState.value.code
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = checkAuthenticationCode(code)
            _uiState.update {
                it.copy(authState = result, loading = false).withErrorIfNeeded(result)
            }
        }
    }

    fun submitPassword() {
        val password = _uiState.value.password
        if (password.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = checkAuthenticationPassword(password)
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