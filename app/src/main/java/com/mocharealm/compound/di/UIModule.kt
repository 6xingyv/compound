package com.mocharealm.compound.di

import com.mocharealm.compound.ui.screen.chat.ChatViewModel
import com.mocharealm.compound.ui.screen.me.MeViewModel
import com.mocharealm.compound.ui.screen.msglist.MsgListViewModel
import com.mocharealm.compound.ui.screen.signin.SignInViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel {
        SignInViewModel(
            setAuthenticationPhoneNumber = get(),
            checkAuthenticationCode = get(),
            checkAuthenticationPassword = get(),
            getAuthenticationState = get()
        )
    }
    viewModel {
        MeViewModel(
            getCurrentUser = get(),
            logout = get()
        )
    }
    viewModel {
        MsgListViewModel(
            getChats = get(),
            downloadFile = get()
        )
    }
    viewModel {
        ChatViewModel(
            getChatMessages = get(),
            downloadFile = get()
        )
    }
}