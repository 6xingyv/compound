package com.mocharealm.compound.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.GetAuthenticationStateUseCase
import com.mocharealm.tci18n.core.LocalTdStringProvider
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tci18n.core.tdI18nNavEntryDecorator
import com.mocharealm.tci18n.generated.TdManifest
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

@Immutable
@Serializable
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data class Chat(val chatId: Long) : Screen

    @Serializable
    data object Intro : Screen

    @Serializable
    data object SignIn : Screen

}

@Stable
class Navigator(private val backStack: MutableList<NavKey>) {
    fun push(key: NavKey) = backStack.add(key)
    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun replaceAll(key: NavKey) {
        backStack.clear(); backStack.add(key)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator found!") }

@OptIn(KoinExperimentalAPI::class)
@Composable
fun AppNav() {
    val backStack = rememberNavBackStack(Screen.Home)

    val navigator = remember { Navigator(backStack) }
    val getAuthState: GetAuthenticationStateUseCase = koinInject()

    LaunchedEffect(Unit) {
        val authState = getAuthState()
        Log.d("AppNav", "authState: $authState")
        if (authState !is AuthState.Ready) {
            navigator.replaceAll(Screen.Intro)
        }
    }

    val onBack = remember(navigator) { { navigator.pop() } }

    val rawEntryProvider = koinEntryProvider<NavKey>()
    val currentEntryProvider by rememberUpdatedState(rawEntryProvider)
    val entryProvider = remember {
        { key: NavKey -> currentEntryProvider(key) }
    }

    val tdStringProvider: TdStringProvider = koinInject()
    val i18nDecorator = remember(tdStringProvider) {
        tdI18nNavEntryDecorator<NavKey>(
            provider = tdStringProvider,
            getKeys = { pageId -> TdManifest.getKeys(pageId) }
        )
    }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalTdStringProvider provides tdStringProvider,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = onBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                i18nDecorator,
            ),
            entryProvider = entryProvider,
        )
    }
}

