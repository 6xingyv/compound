package com.mocharealm.compound.domain.interceptor.settings

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.settings.PrivacySettingsModuleToken
import com.mocharealm.tcsettings.core.InterceptorResult
import com.mocharealm.tcsettings.core.SettingsInterceptor
import com.mocharealm.tcsettings.core.SettingsToken
import org.drinkless.tdlib.TdApi

@SettingsToken(PrivacySettingsModuleToken.SyncContacts::class)
class SyncContactsInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<Boolean> {
    override suspend fun intercept(newValue: Boolean): InterceptorResult {
        return tdLibDataSource.sendSafe(TdApi.SetOption("sync_contacts", TdApi.OptionValueBoolean(newValue))).fold(
            onSuccess = { InterceptorResult.Success },
            onFailure = { InterceptorResult.Failure(throwable = it) }
        )
    }
}

@SettingsToken(PrivacySettingsModuleToken.SuggestFrequentContacts::class)
class SuggestFrequentContactsInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<Boolean> {
    override suspend fun intercept(newValue: Boolean): InterceptorResult {
        return tdLibDataSource.sendSafe(TdApi.SetOption("suggest_contacts", TdApi.OptionValueBoolean(newValue))).fold(
            onSuccess = { InterceptorResult.Success },
            onFailure = { InterceptorResult.Failure(throwable = it) }
        )
    }
}
