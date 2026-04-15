package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule("Language")
interface LanguageSettingsModule {
    @SettingItem
    val languageCode: String

    @SettingItem
    val translateMessages: Boolean
}
