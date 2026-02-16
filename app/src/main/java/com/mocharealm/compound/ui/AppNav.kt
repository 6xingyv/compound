package com.mocharealm.compound.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.GetAuthenticationStateUseCase
import com.mocharealm.compound.ui.screen.msglist.MsgListScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.HorizontalSplit
import top.yukonga.miuix.kmp.icon.extended.Settings

@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Chat(val chatId: Long) : Screen

    @Serializable
    data object SignIn : Screen
}

class Navigator(val backStack: MutableList<NavKey>) {
    fun push(key: NavKey) {
        backStack.add(key)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun replaceAll(key: NavKey) {
        backStack.clear(); backStack.add(key)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator found!") }

private object AppConstants {
    const val MESSAGES_PAGE = 0
    const val PROFILE_PAGE = 1
    const val PAGE_COUNT = 2

    val PAGE_TITLES = listOf("Messages", "Contacts", "Profile")
}

@OptIn(KoinExperimentalAPI::class)
@Composable
fun AppNav() {
    val backStack = rememberNavBackStack(Screen.Home)
    val navigator = remember(backStack) { Navigator(backStack) }
    val getAuthState: GetAuthenticationStateUseCase = koinInject()

    LaunchedEffect(Unit) {
        val authState = getAuthState()
        if (authState !is AuthState.Ready) {
            if (backStack.size == 1 && backStack.first() == Screen.Home) {
                backStack[0] = Screen.SignIn
            } else {
                backStack.add(Screen.SignIn)
            }
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavDisplay(
            backStack = backStack,
            onBack = { navigator.pop() },
            entryProvider = koinEntryProvider<NavKey>(),
        )
    }
}

@Composable
internal fun HomeScreen() {
    val pagerState = rememberPagerState(pageCount = { AppConstants.PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var msgListRefreshSignal by remember { mutableIntStateOf(0) }

    val backStackSize = navigator.backStack.size
    LaunchedEffect(backStackSize) {
        if (backStackSize == 1 && msgListRefreshSignal >= 0) {
            msgListRefreshSignal++
        }
    }

    val navigationItems = remember {
        listOf(
            NavigationItem(AppConstants.PAGE_TITLES[0], MiuixIcons.HorizontalSplit),
            NavigationItem(AppConstants.PAGE_TITLES[1], MiuixIcons.Contacts),
            NavigationItem(AppConstants.PAGE_TITLES[2], MiuixIcons.Settings),
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
//            TopAppBar(
//                title = AppConstants.PAGE_TITLES[pagerState.currentPage],
//                scrollBehavior = topAppBarScrollBehavior,
//            )
        },
        bottomBar = {
            NavigationBar {
                navigationItems.forEachIndexed { index, navigationItem ->
                    NavigationBarItem(
                        pagerState.currentPage == index,
                        {
                            coroutineScope.launch {
                                pagerState.scrollToPage(index)
                            }
                        },
                        navigationItem.icon,
                        navigationItem.label
                    )
                }
            }
        },
        popupHost = {},
    ) { innerPadding ->
        MsgListScreen(
            padding = innerPadding,
            onChatClick = { chatId ->
                navigator.push(Screen.Chat(chatId))
            },
            refreshSignal = msgListRefreshSignal,
        )
    }
}