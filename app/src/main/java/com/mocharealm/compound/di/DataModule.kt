package com.mocharealm.compound.di

import com.mocharealm.compound.data.source.IcuPersonNameFormatterRepositoryImpl
import com.mocharealm.compound.data.source.PhoneFormatRepositoryImpl
import com.mocharealm.compound.data.source.TelegramRepositoryImpl
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository
import com.mocharealm.compound.domain.repository.PhoneFormatRepository
import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tci18n.core.tdLangPackId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.Object
import org.koin.dsl.module
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val dataModule = module {
    single { MutableSharedFlow<TdApi.Update>(replay = 500, extraBufferCapacity = 500) }
    single<SharedFlow<TdApi.Update>> { get<MutableSharedFlow<TdApi.Update>>() }
    single {
        val updateFlow = get<MutableSharedFlow<TdApi.Update>>()

        Client.create(
            { obj: Object? ->
                if (obj is TdApi.Update) {
                    updateFlow.tryEmit(obj)
                }
            },
            { _: Throwable? -> },
            { _: Throwable? -> }
        )
    }
    single<TelegramRepository> {
        TelegramRepositoryImpl(get(), get(), get())
    }
    single<PhoneFormatRepository> {
        PhoneFormatRepositoryImpl(get())
    }
    single<PersonNameFormatterRepository> {
        IcuPersonNameFormatterRepositoryImpl()
    }
    single {
        val client: Client = get()
        TdStringProvider { keys ->
            val langPackId = tdLangPackId(Locale.getDefault())
            // Fire and forget synchronization so it doesn't block offline loading
            client.send(TdApi.SynchronizeLanguagePack(langPackId)) { }

            // Now fetch the locally cached strings immediately
            suspendCancellableCoroutine { cont ->
                client.send(
                    TdApi.GetLanguagePackStrings(langPackId, keys.toTypedArray()),
                    { result: Object? ->
                        when (result) {
                            is TdApi.LanguagePackStrings -> {
                                val map = mutableMapOf<String, com.mocharealm.tci18n.core.TdStringValue>()
                                for (str in result.strings) {
                                    val value = when (val v = str.value) {
                                        is TdApi.LanguagePackStringValueOrdinary -> {
                                            com.mocharealm.tci18n.core.TdStringValue.Ordinary(v.value)
                                        }
                                        is TdApi.LanguagePackStringValuePluralized -> {
                                            com.mocharealm.tci18n.core.TdStringValue.Pluralized(
                                                zero = v.zeroValue,
                                                one = v.oneValue,
                                                two = v.twoValue,
                                                few = v.fewValue,
                                                many = v.manyValue,
                                                other = v.otherValue
                                            )
                                        }
                                        else -> null
                                    }
                                    if (value != null) map[str.key] = value
                                }
                                cont.resume(map)
                            }
                            is TdApi.Error -> cont.resumeWithException(Exception(result.message))
                            else -> cont.resume(emptyMap())
                        }
                    },
                    { e: Throwable? ->
                        cont.resumeWithException(e ?: Exception("Unknown error"))
                    }
                )
            }
        }
    }
}
