package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface PrivacySettingsModule {
    @SettingItem
    @DefaultValue("false")
    val passcodeLock: Boolean

    @SettingItem
    @DefaultValue("false")
    val twoStepVerification: Boolean

    @SettingItem
    @DefaultValue("true")
    val syncContacts: Boolean

    @SettingItem
    @DefaultValue("true")
    val suggestFrequentContacts: Boolean
}
