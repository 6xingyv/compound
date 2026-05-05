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
import com.mocharealm.compound.domain.model.settings.ChatSettingsModuleToken
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.modifier.surface
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun ChatSettingsFragment() {
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
            // 聊天列表设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "聊天列表",
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        SettingRow(
                            title = "滑动手势",
                            subtitle = "滑动归档聊天"
                        ) {
                            ChatSettingsModuleToken.ListSwipeGesture.Render()
                        }
                        SettingRow(
                            title = "置顶归档",
                            subtitle = "将归档聊天置顶显示"
                        ) {
                            ChatSettingsModuleToken.ArchivePinned.Render()
                        }
                        SettingRow(
                            title = "始终显示归档",
                            subtitle = "始终显示归档聊天文件夹"
                        ) {
                            ChatSettingsModuleToken.ArchiveAlwaysVisible.Render()
                        }
                        SettingRow(
                            title = "显示链接预览",
                            subtitle = "在聊天中显示链接预览"
                        ) {
                            ChatSettingsModuleToken.ShowLinkPreviews.Render()
                        }
                    }
                }
            }

            // 视频播放器设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "视频播放器",
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        SettingRow(
                            title = "手势控制",
                            subtitle = "启用播放器手势控制"
                        ) {
                            ChatSettingsModuleToken.PlayerGesturesEnabled.Render()
                        }
                        SettingRow(
                            title = "双击快进",
                            subtitle = "双击屏幕快进"
                        ) {
                            ChatSettingsModuleToken.PlayerDoubleTapSeekEnabled.Render()
                        }
                        SettingRow(
                            title = "快进时长",
                            subtitle = "设置双击快进的秒数"
                        ) {
                            ChatSettingsModuleToken.PlayerSeekDuration.Render()
                        }
                        SettingRow(
                            title = "缩放",
                            subtitle = "启用视频缩放功能"
                        ) {
                            ChatSettingsModuleToken.PlayerZoomEnabled.Render()
                        }
                    }
                }
            }

            // Emoji 设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Emoji",
                        style = MiuixTheme.textStyles.footnote1,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    Column(
                        Modifier.surface(
                            ContinuousRoundedRectangle(16.dp),
                            surfaceColor
                        )
                    ) {
                        SettingRow(
                            title = "Emoji 字体",
                            subtitle = "选择 Emoji 显示样式"
                        ) {
                            ChatSettingsModuleToken.EmojiFont.Render()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MiuixTheme.textStyles.body1)
            Text(
                subtitle,
                style = MiuixTheme.textStyles.body2,
                modifier = Modifier.alpha(0.6f)
            )
        }
        content()
    }
}
