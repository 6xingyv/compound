package com.mocharealm.compound.domain.model

sealed class AuthState {
    object WaitingForPhoneNumber : AuthState()
    object WaitingForOtp : AuthState()
    object WaitingForPassword : AuthState()
    data class Ready(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
