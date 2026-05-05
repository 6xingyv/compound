package com.mocharealm.compound.domain.model.settings

import com.mocharealm.tcsettings.core.DefaultValue
import com.mocharealm.tcsettings.core.SettingItem
import com.mocharealm.tcsettings.core.SettingsModule

@SettingsModule
interface ChatSettingsModule {
    // 聊天列表设置
    @SettingItem
    @DefaultValue("false")
    val listSwipeGesture: Boolean

    @SettingItem
    @DefaultValue("true")
    val archivePinned: Boolean

    @SettingItem
    @DefaultValue("false")
    val archiveAlwaysVisible: Boolean

    @SettingItem
    @DefaultValue("true")
    val showLinkPreviews: Boolean

    // 视频播放器设置
    @SettingItem
    @DefaultValue("true")
    val playerGesturesEnabled: Boolean

    @SettingItem
    @DefaultValue("true")
    val playerDoubleTapSeekEnabled: Boolean

    @SettingItem
    @DefaultValue("10")
    val playerSeekDuration: Int

    @SettingItem
    @DefaultValue("true")
    val playerZoomEnabled: Boolean

    // Emoji 设置
    @SettingItem
    @DefaultValue("16")
    val emojiFont: Int
}
