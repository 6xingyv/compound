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
import com.mocharealm.compound.domain.model.settings.DataSettingsModuleToken
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import com.mocharealm.gaze.ui.modifier.surface
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun DataSettingsFragment() {
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
            // 自动下载设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "自动下载",
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
                            title = "照片 (移动数据)",
                            subtitle = "在移动数据下自动下载照片"
                        ) {
                            DataSettingsModuleToken.AutoDownloadPhotosMobile.Render()
                        }
                        SettingRow(
                            title = "照片 (Wi-Fi)",
                            subtitle = "在 Wi-Fi 下自动下载照片"
                        ) {
                            DataSettingsModuleToken.AutoDownloadPhotosWifi.Render()
                        }
                        SettingRow(
                            title = "视频 (移动数据)",
                            subtitle = "在移动数据下自动下载视频"
                        ) {
                            DataSettingsModuleToken.AutoDownloadVideosMobile.Render()
                        }
                        SettingRow(
                            title = "视频 (Wi-Fi)",
                            subtitle = "在 Wi-Fi 下自动下载视频"
                        ) {
                            DataSettingsModuleToken.AutoDownloadVideosWifi.Render()
                        }
                        SettingRow(
                            title = "文件 (移动数据)",
                            subtitle = "在移动数据下自动下载文件"
                        ) {
                            DataSettingsModuleToken.AutoDownloadFilesMobile.Render()
                        }
                        SettingRow(
                            title = "文件 (Wi-Fi)",
                            subtitle = "在 Wi-Fi 下自动下载文件"
                        ) {
                            DataSettingsModuleToken.AutoDownloadFilesWifi.Render()
                        }
                    }
                }
            }

            // 压缩设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "压缩",
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
                            title = "压缩照片",
                            subtitle = "发送前压缩照片以节省流量"
                        ) {
                            DataSettingsModuleToken.CompressPhotos.Render()
                        }
                        SettingRow(
                            title = "压缩视频",
                            subtitle = "发送前压缩视频以节省流量"
                        ) {
                            DataSettingsModuleToken.CompressVideos.Render()
                        }
                    }
                }
            }

            // 自动播放设置
            item {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "自动播放",
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
                            title = "自动播放视频",
                            subtitle = "在聊天中自动播放视频"
                        ) {
                            DataSettingsModuleToken.AutoPlayVideos.Render()
                        }
                        SettingRow(
                            title = "自动播放 GIF",
                            subtitle = "在聊天中自动播放 GIF"
                        ) {
                            DataSettingsModuleToken.AutoPlayGifs.Render()
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
