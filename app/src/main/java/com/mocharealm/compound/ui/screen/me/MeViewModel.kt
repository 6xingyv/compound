package com.mocharealm.compound.ui.screen.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.usecase.GetCurrentUserUseCase
import com.mocharealm.compound.domain.usecase.LogoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MeUiState(
    val user: User? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class MeViewModel(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logout: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            getCurrentUser()
                .fold(
                    onSuccess = { user ->
                        _uiState.update { it.copy(user = user, loading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, loading = false) }
                    }
                )
        }
    }

    fun logoutUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            logout()
                .fold(
                    onSuccess = {
                        _uiState.update { it.copy(user = null, loading = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message, loading = false) }
                    }
                )
        }
    }
}