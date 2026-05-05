package com.mocharealm.compound.domain.interceptor.settings

import com.mocharealm.compound.data.source.remote.TdLibDataSource
import com.mocharealm.compound.domain.model.settings.LanguageSettingsModuleToken
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tcsettings.core.InterceptorResult
import com.mocharealm.tcsettings.core.SettingsInterceptor
import com.mocharealm.tcsettings.core.SettingsToken
import org.drinkless.tdlib.TdApi

@SettingsToken(LanguageSettingsModuleToken.LanguageCode::class)
class LanguageCodeInterceptor(
    private val tdLibDataSource: TdLibDataSource,
    private val tdStringProvider: TdStringProvider
) : SettingsInterceptor<String> {
    override suspend fun intercept(newValue: String): InterceptorResult {
        // 同步语言设置到 TDLib
        val result = tdLibDataSource.sendSafe(TdApi.SetOption("language_pack_id", TdApi.OptionValueString(newValue)))
        return result.fold(
            onSuccess = {
                // 语言更改后，刷新 tci18n 字符串缓存
                tdStringProvider.clearCacheAndReload()
                InterceptorResult.Success
            },
            onFailure = { InterceptorResult.Failure(throwable = it) }
        )
    }
}

@SettingsToken(LanguageSettingsModuleToken.TranslateMessages::class)
class TranslateMessagesInterceptor(
    private val tdLibDataSource: TdLibDataSource
) : SettingsInterceptor<Boolean> {
    override suspend fun intercept(newValue: Boolean): InterceptorResult {
        // TDLib 的翻译功能可能需要其他 API
        // 目前返回成功
        return InterceptorResult.Success
    }
}
