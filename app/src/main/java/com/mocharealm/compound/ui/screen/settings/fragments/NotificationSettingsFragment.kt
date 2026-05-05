package com.mocharealm.compound.ui.screen.settings.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mocharealm.compound.domain.model.settings.NotificationSettingsModuleToken
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.modifier.surface
import com.mocharealm.tci18n.core.tdString
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun NotificationSettingsFragment() {
    val inDark = isSystemInDarkTheme()
    val surfaceColor =
        if (inDark) MiuixTheme.colorScheme.surfaceContainer else MiuixTheme.colorScheme.surface
    val surfaceContainerColor =
        if (inDark) MiuixTheme.colorScheme.surface else MiuixTheme.colorScheme.surfaceContainer

    Scaffold(
        Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .background(surfaceContainerColor)
                .fillMaxSize()
                .scrollEndHaptic(),
            contentPadding = innerPadding,
            overscrollEffect = null,
        ) {
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tdString("SettingsNotifications"),
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Private Chats", style = MiuixTheme.textStyles.body1)
                                Text(
                                    "Notifications for private messages",
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.alpha(0.6f)
                                )
                            }
                            NotificationSettingsModuleToken.PrivateChats.Render()
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Groups", style = MiuixTheme.textStyles.body1)
                                Text(
                                    "Notifications for group messages",
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.alpha(0.6f)
                                )
                            }
                            NotificationSettingsModuleToken.Groups.Render()
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Channels", style = MiuixTheme.textStyles.body1)
                                Text(
                                    "Notifications for channel updates",
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.alpha(0.6f)
                                )
                            }
                            NotificationSettingsModuleToken.Channels.Render()
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Stories", style = MiuixTheme.textStyles.body1)
                                Text(
                                    "Notifications for new stories",
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.alpha(0.6f)
                                )
                            }
                            NotificationSettingsModuleToken.Stories.Render()
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Contact Joined Telegram", style = MiuixTheme.textStyles.body1)
                                Text(
                                    "Notify when contacts join Telegram",
                                    style = MiuixTheme.textStyles.body2,
                                    modifier = Modifier.alpha(0.6f)
                                )
                            }
                            NotificationSettingsModuleToken.ContactJoinedTelegram.Render()
                        }
                    }
                }
            }
        }
    }
}
