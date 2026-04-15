package com.mocharealm.compound.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.ui.nav.LocalNavigator
import com.mocharealm.compound.ui.nav.Screen
import com.mocharealm.compound.ui.screen.settings.fragments.ChatSettingsFragment
import com.mocharealm.compound.ui.screen.settings.fragments.DataSettingsFragment
import com.mocharealm.compound.ui.screen.settings.fragments.DevicesSettingsFragment
import com.mocharealm.compound.ui.screen.settings.fragments.LanguageSettingsFragment
import com.mocharealm.compound.ui.screen.settings.fragments.NotificationSettingsFragment
import com.mocharealm.compound.ui.screen.settings.fragments.PrivacySettingsFragment
import com.mocharealm.compound.ui.util.MarkdownTransformation
import com.mocharealm.compound.ui.util.PaddingValuesSide
import com.mocharealm.compound.ui.util.takeOnly
import com.mocharealm.gaze.capsule.ContinuousCapsule
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.layerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.backdrops.rememberLayerBackdrop
import com.mocharealm.gaze.glassy.liquid.effect.effects.blur
import com.mocharealm.gaze.glassy.liquid.effect.effects.lens
import com.mocharealm.gaze.glassy.liquid.effect.effects.vibrancy
import com.mocharealm.gaze.icons.Bell_Fill
import com.mocharealm.gaze.icons.Chart_Pie_Fill
import com.mocharealm.gaze.icons.Globe_Fill
import com.mocharealm.gaze.icons.Hand_Raised_Fill
import com.mocharealm.gaze.icons.Ipad_Landscape_And_Iphone
import com.mocharealm.gaze.icons.Key_Fill
import com.mocharealm.gaze.icons.Magnifyingglass
import com.mocharealm.gaze.icons.Message_Fill
import com.mocharealm.gaze.icons.Questionmark_Circle_Fill
import com.mocharealm.gaze.icons.SFIcons
import com.mocharealm.gaze.ui.composable.LiquidSurface
import com.mocharealm.gaze.ui.composable.TextField
import com.mocharealm.gaze.ui.modifier.surface
import com.mocharealm.tci18n.core.tdLangPackId
import com.mocharealm.tci18n.core.tdString
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

data class SettingsScreenItem(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val colors: Pair<Color, Color>,
    val route: Screen.Settings? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(destination: Screen.SettingsDestination = Screen.SettingsDestination.Root) {
    when (destination) {
        Screen.SettingsDestination.Chat -> {
            ChatSettingsFragment()
            return
        }

        Screen.SettingsDestination.Privacy -> {
            PrivacySettingsFragment()
            return
        }

        Screen.SettingsDestination.Notifications -> {
            NotificationSettingsFragment()
            return
        }

        Screen.SettingsDestination.Data -> {
            DataSettingsFragment()
            return
        }

        Screen.SettingsDestination.Language -> {
            LanguageSettingsFragment()
            return
        }

        Screen.SettingsDestination.Devices -> {
            DevicesSettingsFragment()
            return
        }

        Screen.SettingsDestination.Root -> Unit
    }

    val navigator = LocalNavigator.current
    val inDark = isSystemInDarkTheme()
    val surfaceColor =
        if (inDark) MiuixTheme.colorScheme.surfaceContainer else MiuixTheme.colorScheme.surface
    val surfaceContainerColor =
        if (inDark) MiuixTheme.colorScheme.surface else MiuixTheme.colorScheme.surfaceContainer

    val layerBackdrop = rememberLayerBackdrop {
        drawRect(surfaceContainerColor)
        drawContent()
    }

    val settingsItems = listOf(
        SettingsScreenItem(
            tdString("SettingsChat"),
            tdString("SettingsChatInfo"),
            SFIcons.Message_Fill,
            Color(0xFFf09d1a) to Color(0xFFE16C12),
            Screen.Settings(Screen.SettingsDestination.Chat)
        ),
        SettingsScreenItem(
            tdString("SettingsPrivacySecurity"),
            tdString("SettingsPrivacySecurityInfo"),
            SFIcons.Key_Fill,
            Color(0xFF56c845) to Color(0xFF29b536),
            Screen.Settings(Screen.SettingsDestination.Privacy)
        ),
        SettingsScreenItem(
            tdString("SettingsNotifications"),
            tdString("SettingsNotificationsInfo"),
            SFIcons.Bell_Fill,
            Color(0xFFf15257) to Color(0xFFd93d56),
            Screen.Settings(Screen.SettingsDestination.Notifications)
        ),
        SettingsScreenItem(
            tdString("SettingsData"),
            tdString("SettingsDataInfo"),
            SFIcons.Chart_Pie_Fill,
            Color(0xFF4f82ed) to Color(0xFF3768e2),
            Screen.Settings(Screen.SettingsDestination.Data)
        ),
        SettingsScreenItem(
            tdString("SettingsDevices"),
            tdString("SettingsDevicesInfo"),
            SFIcons.Ipad_Landscape_And_Iphone,
            Color(0xFF30bfcd) to Color(0xFF209cc7),
            Screen.Settings(Screen.SettingsDestination.Devices)
        ),
        SettingsScreenItem(
            tdString("SettingsLanguage"),
            tdLangPackId(java.util.Locale.getDefault()),
            SFIcons.Globe_Fill,
            Color(0xFFc26cf3) to Color(0xFFa056df),
            Screen.Settings(Screen.SettingsDestination.Language)
        )
    )

    val helpItems = listOf(
        SettingsScreenItem(
            tdString("AskAQuestion"),
            null,
            SFIcons.Hand_Raised_Fill,
            Color(0xFFf09d1a) to Color(0xFFE16C12)
        ),
        SettingsScreenItem(
            tdString("TelegramFaq"),
            "https://telegram.org/faq",
            SFIcons.Questionmark_Circle_Fill,
            Color(0xFF4f82ed) to Color(0xFF3768e2)
        )
    )

    Scaffold(
        Modifier
            .fillMaxSize(),
        bottomBar = {
            Row(
                Modifier
                    .imePadding()
                    .drawWithCache {
                        onDrawBehind {
                            drawRect(
                                Brush.verticalGradient(
                                    0f to surfaceContainerColor.copy(0f),
                                    1f to surfaceContainerColor.copy(1f)
                                )
                            )
                        }
                    }
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
                    .padding(
                        WindowInsets.captionBar.asPaddingValues().takeOnly(PaddingValuesSide.Bottom)
                    )
            ) {
                LiquidSurface(
                    layerBackdrop,
                    Modifier
                        .height(64.dp)
                        .fillMaxWidth(),
                    Modifier,
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
                                                "SearchInSettings"
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
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .layerBackdrop(layerBackdrop)
                .background(surfaceContainerColor)
                .fillMaxSize()
                .imeNestedScroll()
                .scrollEndHaptic(),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item("settings") {
                Column(
                    Modifier
                        .padding(16.dp)
                        .surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                ) {
                    settingsItems.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = item.route != null) {
                                    item.route?.let { navigator.push(it) }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .padding(16.dp)
                                    .clip(
                                        ContinuousRoundedRectangle(12.dp)
                                    )
                                    .background(
                                        Brush.verticalGradient(
                                            0f to item.colors.first,
                                            1f to item.colors.second
                                        )
                                    )
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    item.icon,
                                    null,
                                    Modifier
                                        .size(16.dp)
                                        .graphicsLayer {
                                            blendMode = BlendMode.Plus
                                        }
                                        .drawWithCache {
                                            onDrawWithContent {
                                                drawContent()
                                                drawRect(
                                                    Brush.verticalGradient(
                                                        0f to Color.White.copy(0.5f),
                                                        1f to Color.White.copy(0.75f)
                                                    ),
                                                    blendMode = BlendMode.DstIn
                                                )
                                            }
                                        },
                                    tint = Color.White
                                )
                            }
                            Column {
                                Text(item.title, style = MiuixTheme.textStyles.body1)
                                item.subtitle?.let { subtitle ->
                                    Text(
                                        subtitle,
                                        style = MiuixTheme.textStyles.body2,
                                        modifier = Modifier.alpha(0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item("help") {
                Column(
                    Modifier
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tdString("SettingsHelp"),
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        helpItems.forEachIndexed { index, item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    Modifier
                                        .padding(16.dp)
                                        .clip(
                                            ContinuousRoundedRectangle(12.dp)
                                        )
                                        .background(
                                            Brush.verticalGradient(
                                                0f to item.colors.first,
                                                1f to item.colors.second
                                            )
                                        )
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        item.icon,
                                        null,
                                        Modifier
                                            .size(16.dp)
                                            .graphicsLayer {
                                                blendMode = BlendMode.Plus
                                            }
                                            .drawWithCache {
                                                onDrawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        Brush.verticalGradient(
                                                            0f to Color.White.copy(0.5f),
                                                            1f to Color.White.copy(0.75f)
                                                        ),
                                                        blendMode = BlendMode.DstIn
                                                    )
                                                }
                                            },
                                        tint = Color.White
                                    )
                                }
                                Column {
                                    Text(item.title, style = MiuixTheme.textStyles.body1)
                                    item.subtitle?.let { subtitle ->
                                        Text(
                                            subtitle,
                                            style = MiuixTheme.textStyles.body2,
                                            modifier = Modifier.alpha(0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}