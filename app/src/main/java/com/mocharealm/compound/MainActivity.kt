package com.mocharealm.compound

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mocharealm.compound.ui.nav.AppNavViewModel
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.Navigator
import com.mocharealm.compound.ui.theme.CompoundTheme
import com.mocharealm.gaze.nav.SinglePaneSceneStrategy
import com.mocharealm.gaze.nav.rememberListDetailSceneStrategy
import com.mocharealm.tci18n.core.LocalTdStringProvider
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tci18n.core.tdI18nNavEntryDecorator
import com.mocharealm.compound.ui.util.LocalPersonNameFormatter
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

class MainActivity : ComponentActivity() {
    private val navViewModel: AppNavViewModel by viewModel()

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !navViewModel.isReady.value }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Xiaomi nav bar workaround
        if (Build.MANUFACTURER.lowercase(java.util.Locale.getDefault()) == "xiaomi") {
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        navViewModel.initialize(intent, this)

        setContent {
            CompoundTheme {
                val isReady by navViewModel.isReady.collectAsState()
                if (!isReady) return@CompoundTheme

                val startScreen by navViewModel.startScreen.collectAsState()
                val backStack = rememberNavBackStack(startScreen)
                val navigator = remember { Navigator(backStack) }
                val onBack = remember(navigator) { { navigator.pop() } }

                val tdStringProvider: TdStringProvider = koinInject()
                val nameFormatter: PersonNameFormatterRepository = koinInject()
                val i18nDecorator = remember(tdStringProvider) {
                    tdI18nNavEntryDecorator<NavKey>(
                        provider = tdStringProvider
                    )
                }

                CompositionLocalProvider(
                    LocalNavigator provides navigator,
                    LocalTdStringProvider provides tdStringProvider,
                    LocalPersonNameFormatter provides nameFormatter,
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = onBack,
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(),
                            rememberViewModelStoreNavEntryDecorator(),
                            i18nDecorator,
                        ),
                        entryProvider = koinEntryProvider(),
                        sceneStrategies = listOf(
                            rememberListDetailSceneStrategy(),
                            SinglePaneSceneStrategy()
                        )
                    )
                }
            }
        }
    }
}