package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule("Privacy")
interface PrivacySettingsModule {
    @SettingItem
    val passcodeLock: Boolean

    @SettingItem
    val twoStepVerification: Boolean

    @SettingItem
    val syncContacts: Boolean

    @SettingItem
    val suggestFrequentContacts: Boolean
}
