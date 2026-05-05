package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface DataSettingsModule {
    // 自动下载设置
    @SettingItem
    @DefaultValue("true")
    val autoDownloadPhotosMobile: Boolean

    @SettingItem
    @DefaultValue("true")
    val autoDownloadPhotosWifi: Boolean

    @SettingItem
    @DefaultValue("false")
    val autoDownloadVideosMobile: Boolean

    @SettingItem
    @DefaultValue("true")
    val autoDownloadVideosWifi: Boolean

    @SettingItem
    @DefaultValue("false")
    val autoDownloadFilesMobile: Boolean

    @SettingItem
    @DefaultValue("true")
    val autoDownloadFilesWifi: Boolean

    // 压缩设置
    @SettingItem
    @DefaultValue("true")
    val compressPhotos: Boolean

    @SettingItem
    @DefaultValue("true")
    val compressVideos: Boolean

    // 自动播放设置
    @SettingItem
    @DefaultValue("true")
    val autoPlayVideos: Boolean

    @SettingItem
    @DefaultValue("true")
    val autoPlayGifs: Boolean
}
