package com.mocharealm.compound

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.usecase.auth.GetAuthenticationStateUseCase
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.Navigator
import com.mocharealm.compound.ui.Screen
import com.mocharealm.compound.ui.theme.CompoundTheme
import com.mocharealm.gaze.nav.rememberListDetailSceneStrategy
import com.mocharealm.tci18n.core.LocalTdStringProvider
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tci18n.core.tdI18nNavEntryDecorator
import com.mocharealm.tci18n.generated.TdManifest
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

class MainActivity : ComponentActivity() {
    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.MANUFACTURER.toLowerCase(Locale.current) == "xiaomi") {
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CompoundTheme {
                val backStack = rememberNavBackStack(Screen.Intro)

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
        }
    }
}