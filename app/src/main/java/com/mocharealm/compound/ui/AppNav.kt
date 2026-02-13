package com.mocharealm.compound.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.GetAuthenticationStateUseCase
import com.mocharealm.compound.ui.screen.chat.ChatScreen
import com.mocharealm.compound.ui.screen.contact.ContactScreen
import com.mocharealm.compound.ui.screen.me.MeScreen
import com.mocharealm.compound.ui.screen.msglist.MsgListScreen
import com.mocharealm.compound.ui.screen.signin.SignInScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
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

sealed interface Screen : NavKey {
    data object Home : Screen
    data class Chat(val chatId: Long, val chatTitle: String) : Screen
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
    const val CONTACTS_PAGE = 1
    const val PROFILE_PAGE = 2
    const val PAGE_COUNT = 3

    val PAGE_TITLES = listOf("Messages", "Contacts", "Profile")
}

@Composable
fun AppNav() {
    val backStack = remember { mutableStateListOf<NavKey>(Screen.Home) }
    val navigator = remember(backStack) { Navigator(backStack) }
    val getAuthState: GetAuthenticationStateUseCase = koinInject()

    // Check auth state on startup — navigate to SignIn if not authenticated
    LaunchedEffect(Unit) {
        val authState = getAuthState()
        if (authState !is AuthState.Ready) {
            // Replace Home with SignIn as the initial destination
            if (backStack.size == 1 && backStack.first() == Screen.Home) {
                backStack[0] = Screen.SignIn
            } else {
                backStack.add(Screen.SignIn)
            }
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        val entryProvider = remember(backStack) {
            entryProvider<NavKey> {
                entry(Screen.Home) {
                    HomeScreen()
                }
                entry<Screen.Chat> { screen ->
                    ChatScreen(
                        chatId = screen.chatId,
                        chatTitle = screen.chatTitle
                    )
                }
                entry(Screen.SignIn) { SignInScreen() }
            }
        }

        val entries = rememberDecoratedNavEntries(
            backStack = backStack,
            entryProvider = entryProvider,
        )

        NavDisplay(
            entries = entries,
            onBack = { navigator.pop() },
        )
    }
}

@Composable
private fun HomeScreen() {
    val pagerState = rememberPagerState(pageCount = { AppConstants.PAGE_COUNT })
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    var msgListRefreshSignal by remember { mutableIntStateOf(0) }

    // 监听 backStack 变化，当从 Chat 返回时（stack 缩小到只剩 Home）触发刷新
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
            TopAppBar(
                title = AppConstants.PAGE_TITLES[pagerState.currentPage],
                scrollBehavior = topAppBarScrollBehavior,
            )
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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1,
            overscrollEffect = null,
            pageContent = { page ->
                when (page) {
                    AppConstants.MESSAGES_PAGE -> MsgListScreen(
                        padding = innerPadding,
                        onChatClick = { chatId, chatTitle ->
                            navigator.push(Screen.Chat(chatId, chatTitle))
                        },
                        refreshSignal = msgListRefreshSignal,
                    )

                    AppConstants.CONTACTS_PAGE -> ContactScreen(
                        padding = innerPadding,
                    )

                    AppConstants.PROFILE_PAGE -> MeScreen(
                        padding = innerPadding,
                    )
                }
            },
        )
    }
}