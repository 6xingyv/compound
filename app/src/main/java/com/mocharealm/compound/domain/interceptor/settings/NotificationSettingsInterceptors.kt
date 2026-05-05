package com.mocharealm.compound.domain.interceptor.settings

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.settings.NotificationSettingsModuleToken
import com.mocharealm.tcsettings.core.InterceptorResult
import com.mocharealm.tcsettings.core.SettingsInterceptor
import com.mocharealm.tcsettings.core.SettingsToken
import org.drinkless.tdlib.TdApi

@SettingsToken(NotificationSettingsModuleToken.PrivateChats::class)
class PrivateChatsNotificationInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<Boolean> {
    override suspend fun intercept(newValue: Boolean): InterceptorResult {
        // TDLib 使用 notification_scope 来控制通知
        // 这里需要调用 SetOption 或其他 API
        // TODO
        return InterceptorResult.Success
    }
}

@SettingsToken(NotificationSettingsModuleToken.ContactJoinedTelegram::class)
class ContactJoinedTelegramInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<Boolean> {
    override suspend fun intercept(newValue: Boolean): InterceptorResult {
        return tdLibDataSource.sendSafe(TdApi.SetOption("contact_joined_notification", TdApi.OptionValueBoolean(newValue))).fold(
            onSuccess = { InterceptorResult.Success },
            onFailure = { InterceptorResult.Failure(throwable = it) }
        )
    }
}
