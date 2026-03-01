package com.mocharealm.compound.ui.nav

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.OnBackCompletedFallback
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.auth.GetAuthenticationStateUseCase
import com.mocharealm.gaze.nav.rememberListDetailSceneStrategy
import com.mocharealm.tci18n.core.LocalTdStringProvider
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tci18n.core.tdI18nNavEntryDecorator
import com.mocharealm.tci18n.generated.TdManifest
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider

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
    fun push(key: NavKey, singleTop: Boolean = false) {
        if (singleTop && backStack.lastOrNull()?.javaClass == key.javaClass) {
            backStack.removeLastOrNull()
        }
        backStack.add(key)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    fun replaceAll(key: NavKey) {
        backStack.clear()
        backStack.add(key)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator found!") }

fun Uri?.toScreen(fallback: Screen = Screen.Intro): Screen {
    Log.e("DEEPLINKER", this.toString())
    return fallback
}


@Composable
fun AppNav(viewModel: AppNavViewModel= koinViewModel()) {

    val backStack = rememberNavBackStack(startKey)

    val navigator = remember { Navigator(backStack) }
    val getAuthState: GetAuthenticationStateUseCase = koinInject()

    LaunchedEffect(Unit) {
        val authState = getAuthState()
        Log.d("AppNav", "authState: $authState")
        if (authState is AuthState.Ready) {
            navigator.replaceAll(Screen.Home)
        }
    }

    val onBack = remember(navigator) { { navigator.pop() } }

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
            entryProvider = koinEntryProvider(),
            sceneStrategy = rememberListDetailSceneStrategy(),
        )
    }
}