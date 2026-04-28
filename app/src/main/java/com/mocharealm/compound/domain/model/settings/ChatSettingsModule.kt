package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface ChatSettingsModule {
    @SettingItem
    @DefaultValue("false")
    val listSwipeGesture: Boolean

    @SettingItem
    @DefaultValue("16")
    val emojiFont: Int
}
