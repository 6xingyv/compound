package com.mocharealm.compound.ui.nav

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavKey
import com.mocharealm.compound.domain.model.SharePayload
import kotlinx.serialization.Serializable

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

    @Serializable
    data class SharePicker(val payload: SharePayload) : Screen
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
