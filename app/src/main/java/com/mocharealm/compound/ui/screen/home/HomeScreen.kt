package com.mocharealm.compound.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mocharealm.compound.ui.LocalNavigator
import com.mocharealm.compound.ui.Screen
import com.mocharealm.compound.ui.composable.Avatar
import com.mocharealm.compound.ui.screen.me.MeScreen
import com.mocharealm.compound.ui.screen.me.MeViewModel
import com.mocharealm.compound.ui.screen.msglist.MsgListScreen
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.BottomTab
import com.mocharealm.gaze.ui.composable.BottomTabs
import com.mocharealm.gaze.ui.composable.Button
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class TopLevelNav(
    val title: String,
    val icon: @Composable () -> Unit
)

@Composable
fun HomeScreen(
    viewModel: MeViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val surfaceColor = MiuixTheme.colorScheme.surface

    val layerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val navigationItems = remember {
        listOf(
            TopLevelNav(
                "Compound",
                {
                    Icon(SFIcons.Message_Fill, null)
                },
            ),
            TopLevelNav(
                "Me",
                {
                    Avatar(
                        state.user?.firstName?.take(2) ?: "Me",
                        Modifier.size(24.dp),
                        state.user?.profilePhotoUrl
                    )
                }
            )
        )
    }
    val selectedIndex = remember { mutableIntStateOf(0) }

    val navigator = LocalNavigator.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {},
        bottomBar = {
            Row(
                Modifier
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to surfaceColor.copy(0f),
                                    1f to surfaceColor.copy(1f)
                                )
                            )
                        }
                    }
                    .navigationBarsPadding()
                    .captionBarPadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BottomTabs(
                    { selectedIndex.intValue },
                    { selectedIndex.intValue = it },
                    layerBackdrop,
                    navigationItems.size
                ) {
                    navigationItems.forEachIndexed { index, item ->
                        BottomTab({ selectedIndex.intValue = index }) {
                            Column(
                                Modifier
                                    .padding(horizontal = 24.dp)
                                    .align(Alignment.Center),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item.icon()
                                Text(item.title, style = MiuixTheme.textStyles.footnote1)
                            }
                        }
                    }
                }
                Button(
                    {},
                    layerBackdrop,
                    Modifier
                        .size(64.dp)
                ) {
                    Icon(
                        SFIcons.Magnifyingglass,
                        null,
                        Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        },
        popupHost = {},
    ) { innerPadding ->
        AnimatedContent(selectedIndex.intValue, Modifier.layerBackdrop(layerBackdrop)) { index ->
            when (index) {
                0 -> {
                    MsgListScreen(
                        padding = innerPadding,
                        onChatClick = { chatId ->
                            navigator.push(Screen.Chat(chatId))
                        },
                    )
                }

                1 -> {
                    MeScreen(padding = innerPadding)
                }
            }
        }
    }
}