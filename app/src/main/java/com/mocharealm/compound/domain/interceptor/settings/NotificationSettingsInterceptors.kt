package com.mocharealm.compound.domain.interceptor.settings

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.settings.NotificationSettingsModuleToken
import com.mocharealm.tcsettings.core.InterceptorResult
import com.mocharealm.tcsettings.core.SettingsInterceptor
import com.mocharealm.tcsettings.core.SettingsToken
import org.drinkless.tdlib.TdApi

@SettingsToken(NotificationSettingsModuleToken.ContactJoinedTelegram::class)
class ContactJoinedTelegramInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<Boolean> {
    override suspend fun intercept(newValue: Boolean): InterceptorResult {
        // According to TDLib for contact joined setting
        // Often controlled via general scope setting or "contact_joined_notification_enabled" option
        // We'll use a placeholder Option mapping for demonstration
        return tdLibDataSource.sendSafe(TdApi.SetOption("contact_joined_notification_enabled", TdApi.OptionValueBoolean(newValue))).fold(
            onSuccess = { InterceptorResult.Success },
            onFailure = { InterceptorResult.Failure(throwable = it) }
        )
    }
}