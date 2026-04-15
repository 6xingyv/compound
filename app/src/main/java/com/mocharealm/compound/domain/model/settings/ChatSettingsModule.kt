package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule("Chat")
interface ChatSettingsModule {
    @SettingItem
    val listSwipeGesture: Boolean

    @SettingItem
    val emojiFont: Int
}
