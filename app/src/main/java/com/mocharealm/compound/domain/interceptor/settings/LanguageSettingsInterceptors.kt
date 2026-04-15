package com.mocharealm.compound.domain.interceptor.settings

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.settings.LanguageSettingsModuleToken
import com.mocharealm.tcsettings.core.InterceptorResult
import com.mocharealm.tcsettings.core.SettingsInterceptor
import com.mocharealm.tcsettings.core.SettingsToken
import org.drinkless.tdlib.TdApi

@SettingsToken(LanguageSettingsModuleToken.LanguageCode::class)
class LanguageCodeInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<String> {
    override suspend fun intercept(newValue: String): InterceptorResult {
        return tdLibDataSource.sendSafe(TdApi.SetOption("language_pack_id", TdApi.OptionValueString(newValue))).fold(
            onSuccess = { InterceptorResult.Success },
            onFailure = { InterceptorResult.Failure(throwable = it) }
        )
    }
}
