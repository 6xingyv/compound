package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface DataSettingsModule {
    @SettingItem
    @DefaultValue("true")
    val autoDownloadPhotosMobile: Boolean

    @SettingItem
    @DefaultValue("true")
    val autoDownloadPhotosWifi: Boolean

    @SettingItem
    @DefaultValue("true")
    val autoPlayVideos: Boolean
}
