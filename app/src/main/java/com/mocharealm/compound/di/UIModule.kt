package com.mocharealm.compound.di

import com.mocharealm.compound.ui.screen.chat.ChatViewModel
import com.mocharealm.compound.ui.Screen
import com.mocharealm.compound.ui.screen.chat.ChatScreen
import com.mocharealm.compound.ui.screen.home.HomeScreen
import com.mocharealm.compound.ui.screen.home.HomeViewModel
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
            downloadFile = get(),
            subscribeToMessageUpdates = get(),
            getChat = get()
        )
    }
    viewModel { (chatId: Long) ->
        ChatViewModel(
            chatId = chatId,
            getChatMessages = get(),
            downloadFile = get(),
            downloadFileWithProgress = get(),
            sendMessage = get(),
            subscribeToMessageUpdates = get(),
            getChat = get()
        )
    }

    viewModel {
        HomeViewModel(get())
    }

    navigation<Screen.Home> {
        HomeScreen()
    }
    navigation<Screen.Chat> { route ->
        ChatScreen(
            viewModel = koinViewModel(key = "ChatViewModel_${route.chatId}") { parametersOf(route.chatId) }
        )
    }
    navigation<Screen.SignIn> {
        SignInScreen()
    }
}