package com.mocharealm.compound.di

import ChatViewModel
import com.mocharealm.compound.ui.Screen
import com.mocharealm.compound.ui.screen.chat.ChatScreen
import com.mocharealm.compound.ui.screen.me.MeViewModel
import com.mocharealm.compound.ui.screen.msglist.MsgListViewModel
import com.mocharealm.compound.ui.screen.signin.SignInScreen
import com.mocharealm.compound.ui.screen.signin.SignInViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
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
    viewModel { (chatId: Long) ->
        ChatViewModel(
            chatId = chatId,
            getChatMessages = get(),
            downloadFile = get(),
            sendMessage = get(),
            subscribeToMessageUpdates = get(),
            getChat = get()
        )
    }

    navigation<Screen.Home> {
        com.mocharealm.compound.ui.HomeScreen()
    }
    navigation<Screen.Chat> { route ->
        ChatScreen(
            viewModel = koinViewModel { parametersOf(route.chatId) }
        )
    }
    navigation<Screen.SignIn> {
        SignInScreen()
    }
}