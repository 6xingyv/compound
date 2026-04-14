package com.mocharealm.compound.di

import com.mocharealm.compound.ui.nav.AppNavViewModel
import com.mocharealm.compound.ui.nav.DeepLinkHandler
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.Screen
import com.mocharealm.compound.ui.screen.chat.ChatScreen
import com.mocharealm.compound.ui.screen.chat.ChatViewModel
import com.mocharealm.compound.ui.screen.chat.MediaPreviewScreen
import com.mocharealm.compound.ui.screen.chat.MediaPreviewViewModel
import com.mocharealm.compound.ui.screen.home.HomeScreen
import com.mocharealm.compound.ui.screen.home.HomeViewModel
import com.mocharealm.compound.ui.screen.intro.IntroScreen
import com.mocharealm.compound.ui.screen.me.MeViewModel
import com.mocharealm.compound.ui.screen.msglist.ArchivedMsgListScreen
import com.mocharealm.compound.ui.screen.msglist.ArchivedMsgListViewModel
import com.mocharealm.compound.ui.screen.msglist.MsgListViewModel
import com.mocharealm.compound.ui.screen.share.SharePickerScreen
import com.mocharealm.compound.ui.screen.signin.SignInScreen
import com.mocharealm.compound.ui.screen.signin.SignInViewModel
import com.mocharealm.gaze.nav.ListDetailScene.Companion.DETAIL_KEY
import com.mocharealm.gaze.nav.ListDetailScene.Companion.FULLSCREEN_KEY
import com.mocharealm.gaze.nav.ListDetailScene.Companion.LIST_KEY
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
            getChat = get(),
            toggleChatPin = get(),
            toggleChatArchive = get(),
            getCustomEmojiStickers = get()
        )
    }
    viewModel {
        ArchivedMsgListViewModel(
            getChats = get(),
            downloadFile = get(),
            subscribeToMessageUpdates = get(),
            getChat = get(),
            toggleChatArchive = get(),
            getCustomEmojiStickers = get()
        )
    }
    viewModel<ChatViewModel> { (chatId: Long) ->
        ChatViewModel(
            chatId = chatId,
            getChatMessages = get(),
            downloadFile = get(),
            downloadFileWithProgress = get(),
            sendMessage = get(),
            subscribeToMessageUpdates = get(),
            getChat = get(),
            openChat = get(),
            closeChat = get(),
            getInstalledStickerSets = get(),
            getStickerSetStickers = get(),
            sendSticker = get(),
            sendLocation = get(),
            sendFiles = get(),
            setChatDraftMessage = get(),
            saveChatReadPosition = get(),
            getChatReadPosition = get(),
            getCustomEmojiStickers = get(),
            getChatPinnedMessage = get(),
            saveFileToDownloads = get(),
            openFile = get()
        )
    }

    viewModel {
        HomeViewModel(get())
    }

    viewModel<MediaPreviewViewModel> { (chatId: Long, messageId: Long) ->
        MediaPreviewViewModel(
            chatId = chatId,
            messageId = messageId,
            getChatMessages = get(),
            downloadFile = get()
        )
    }

    single {
        DeepLinkHandler(get())
    }

    viewModel {
        AppNavViewModel(get(), get())
    }

    navigation<Screen.Home>(mapOf(LIST_KEY to true)) {
        HomeScreen()
    }
    navigation<Screen.Chat>(mapOf(DETAIL_KEY to true)) { route ->
        ChatScreen(
            viewModel = koinViewModel(key = "ChatViewModel_${route.chatId}") { parametersOf(route.chatId) }
        )
    }
    navigation<Screen.SignIn> {
        SignInScreen()
    }
    navigation<Screen.ArchivedMsgList>(mapOf(LIST_KEY to true)) {
        val navigator = LocalNavigator.current
        ArchivedMsgListScreen(
            onChatClick = { chatId ->
                navigator.push(Screen.Chat(chatId), true)
            }
        )
    }
    navigation<Screen.SharePicker>(mapOf(LIST_KEY to true)) { route ->
        SharePickerScreen(payload = route.payload)
    }
    navigation<Screen.Intro>(mapOf(FULLSCREEN_KEY to true)) {
        IntroScreen()
    }
    navigation<Screen.MediaPreview>(mapOf(DETAIL_KEY to true)) { route ->
        MediaPreviewScreen(viewModel = koinViewModel { parametersOf(route.chatId, route.messageId) })
    }
}
