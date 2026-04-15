package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule("Notification")
interface NotificationSettingsModule {
    @SettingItem
    val privateChats: Boolean

    @SettingItem
    val groups: Boolean

    @SettingItem
    val channels: Boolean

    @SettingItem
    val stories: Boolean
    
    @SettingItem
    val contactJoinedTelegram: Boolean
}
