package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface LanguageSettingsModule {
    @SettingItem
    @DefaultValue("")
    val languageCode: String

    @SettingItem
    @DefaultValue("false")
    val translateMessages: Boolean
}
