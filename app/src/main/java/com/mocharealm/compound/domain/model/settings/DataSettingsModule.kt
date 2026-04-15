package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule("Data")
interface DataSettingsModule {
    @SettingItem
    val autoDownloadPhotosMobile: Boolean

    @SettingItem
    val autoDownloadPhotosWifi: Boolean

    @SettingItem
    val autoPlayVideos: Boolean
}
