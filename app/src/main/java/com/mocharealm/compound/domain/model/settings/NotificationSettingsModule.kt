package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface NotificationSettingsModule {
    @SettingItem
    @DefaultValue("true")
    val privateChats: Boolean

    @SettingItem
    @DefaultValue("true")
    val groups: Boolean

    @SettingItem
    @DefaultValue("true")
    val channels: Boolean

    @SettingItem
    @DefaultValue("true")
    val stories: Boolean

    @SettingItem
    @DefaultValue("true")
    val contactJoinedTelegram: Boolean
}
