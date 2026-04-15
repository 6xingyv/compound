package com.mocharealm.compound.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.captionBarPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mocharealm.compound.ui.composable.base.Avatar
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.Screen
import com.mocharealm.compound.ui.screen.me.MeScreen
import com.mocharealm.compound.ui.screen.me.MeViewModel
import com.mocharealm.compound.ui.screen.msglist.ArchivedMsgListScreen
import com.mocharealm.compound.ui.screen.msglist.MsgListScreen
import com.mocharealm.compound.ui.util.MarkdownTransformation
import com.mocharealm.gaze.capsule.ContinuousCapsule
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.icons.Magnifyingglass
import com.mocharealm.gaze.icons.Message_Fill
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.BottomTab
import com.mocharealm.gaze.ui.composable.BottomTabs
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.gaze.ui.composable.TextField
import com.mocharealm.tci18n.core.tdString
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class TopLevelNav(
    val title: String,
    val icon: @Composable () -> Unit,
    val target: HomePageTarget
)

private enum class HomePageTarget {
    MSG_MAIN,
    MSG_ARCHIVED,
    ME,
    SEARCH
}

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

    val profileString = tdString("MainTabsProfile")

    val navigationItems = remember(state.user, tdString("MainTabsProfile")) {
        listOf(
            TopLevelNav(
                "Compound",
                {
                    Icon(SFIcons.Message_Fill, null)
                },
                HomePageTarget.MSG_MAIN
            ),
            TopLevelNav(
                profileString,
                {
                    Avatar(
                        state.user?.firstName?.take(2) ?: "Me",
                        Modifier.size(24.dp),
                        state.user?.profilePhotoUrl
                    )
                },
                HomePageTarget.ME
            )
        )
    }
    val selectedIndex = remember { mutableIntStateOf(0) }
    val page = remember { mutableStateOf(HomePageTarget.MSG_MAIN) }

    BackHandler(enabled = page.value != HomePageTarget.MSG_MAIN && page.value != HomePageTarget.ME) {
        page.value = HomePageTarget.MSG_MAIN
        selectedIndex.intValue = 0
    }

    val navigator = LocalNavigator.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {},
        bottomBar = {
            Box(
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
                        .imePadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
            ) {
                SearchBarLayout(
                    page.value == HomePageTarget.SEARCH,
                    {
                        BottomTabs(
                            { selectedIndex.intValue },
                            { index ->
                                selectedIndex.intValue = index
                                page.value =
                                    if (index == 0) HomePageTarget.MSG_MAIN else HomePageTarget.ME
                            },
                            layerBackdrop,
                            navigationItems.size
                        ) {
                            navigationItems.forEachIndexed { index, item ->
                                BottomTab({
                                    selectedIndex.intValue = index
                                    page.value = item.target
                                }) {
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
                    },
                    {
                        LiquidSurface(
                            layerBackdrop,
                            Modifier
                                .height(64.dp)
                                .fillMaxWidth(),
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    page.value = HomePageTarget.SEARCH
                                }
                            ),
                            shape = { ContinuousCapsule },
                            effects = {
                                vibrancy()
                                blur(8f.dp.toPx())
                                lens(24f.dp.toPx(), 24f.dp.toPx())
                            },
                            tint = Color.Unspecified,
                            surfaceColor = MiuixTheme.colorScheme.surfaceContainer.copy(0.4f)
                        ) {
                            Row(
                                Modifier
                                    .height(32.dp)
                                    .align(Alignment.Center)
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    SFIcons.Magnifyingglass,
                                    null,
                                    Modifier
                                        .size(32.dp)
                                )
                                val inputState = remember { TextFieldState() }
                                val isDark = isSystemInDarkTheme()
                                AnimatedVisibility(
                                    page.value == HomePageTarget.SEARCH,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    TextField(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        state = inputState,
                                        outputTransformation = MarkdownTransformation,
                                        lineLimits = TextFieldLineLimits.MultiLine(),
                                        padding = 0.dp,
                                        clipRadius = 0.dp,
                                        activeBackgroundColor = Color.Transparent,
                                        inactiveBackgroundColor = Color.Transparent,
                                        activeBorderSize = 0.dp,
                                        inactiveBorderSize = 0.dp,
                                        textStyle = MiuixTheme.textStyles.body1,
                                        decorator = { innerTextField ->
                                            if (inputState.text.isEmpty()) {
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    innerTextField()
                                                    Text(
                                                        tdString(
                                                            "GlobalSearch"
                                                        ),
                                                        modifier = Modifier.graphicsLayer {
                                                            blendMode =
                                                                if (isDark) BlendMode.Plus else BlendMode.Multiply
                                                            alpha = 0.6f
                                                        },
                                                        style = MiuixTheme.textStyles.body1,
                                                    )
                                                }
                                            } else innerTextField()
                                        })
                                }
                            }
                        }
                    }
                )
            }
        },
        popupHost = {},
    ) { innerPadding ->
        AnimatedContent(
            targetState = page.value,
            modifier = Modifier.layerBackdrop(layerBackdrop),
            transitionSpec = {
                (fadeIn(animationSpec = tween(220))
                        + scaleIn(initialScale = 0.92f, animationSpec = tween(220)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(90))
                                + scaleOut(targetScale = 1.08f, animationSpec = tween(220))
                    )
            },
            label = "PageTransition"
        ) { targetPage ->
            when (targetPage) {
                HomePageTarget.MSG_MAIN -> {
                    MsgListScreen(
                        padding = innerPadding,
                        onOpenArchived = { page.value = HomePageTarget.MSG_ARCHIVED },
                        onChatClick = { chatId -> navigator.push(Screen.Chat(chatId), true) }
                    )
                }

                HomePageTarget.MSG_ARCHIVED -> {
                    ArchivedMsgListScreen(
                        padding = innerPadding,
                        onChatClick = { chatId -> navigator.push(Screen.Chat(chatId), true) }
                    )
                }

                HomePageTarget.ME -> {
                    MeScreen(padding = innerPadding)
                }

                HomePageTarget.SEARCH -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Search Screen")
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBarLayout(
    inSearchMode: Boolean,
    tabBar: @Composable () -> Unit,
    searchBar: @Composable () -> Unit
) {
    // 只有一个动画变量驱动
    val progress by animateFloatAsState(
        targetValue = if (inSearchMode) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    Layout(
        content = {
            Box(Modifier.layoutId("tabBar")) { tabBar() }
            Box(Modifier.layoutId("searchBar")) { searchBar() }
        }
    ) { measurables, constraints ->
        val tabMeasurable = measurables.first { it.layoutId == "tabBar" }
        val buttonMeasurable = measurables.first { it.layoutId == "searchBar" }

        val minWidth = 64.dp.roundToPx()
        val maxWidth = constraints.maxWidth
        val currentWidth = lerp(minWidth, maxWidth, progress)

        val buttonPlaceable = buttonMeasurable.measure(
            constraints.copy(minWidth = currentWidth, maxWidth = currentWidth)
        )

        val tabPlaceable = tabMeasurable.measure(constraints.copy(minWidth = 0))

        layout(constraints.maxWidth, buttonPlaceable.height) {
            tabPlaceable.placeRelativeWithLayer(x = 0, y = 0) {
                translationX = -(tabPlaceable.width * progress)
                alpha = 1f - progress
            }

            val buttonX = lerp(constraints.maxWidth - minWidth, 0, progress)
            buttonPlaceable.placeRelative(buttonX, 0)
        }
    }
}