package com.mocharealm.compound.di

import com.mocharealm.compound.data.source.TelegramRepositoryImpl
import com.mocharealm.compound.domain.repository.TelegramRepository
import com.mocharealm.compound.domain.usecase.CheckAuthenticationCodeUseCase
import com.mocharealm.compound.domain.usecase.CheckAuthenticationPasswordUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.GetAuthenticationStateUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.GetCurrentUserUseCase
import com.mocharealm.compound.domain.usecase.LogoutUseCase
import com.mocharealm.compound.domain.usecase.SendMessageUseCase
import com.mocharealm.compound.domain.usecase.SetAuthenticationPhoneNumberUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.Object
import org.koin.dsl.module

val dataModule = module {
    single { MutableSharedFlow<TdApi.UpdateFile>(extraBufferCapacity = 64) }
    single<SharedFlow<TdApi.UpdateFile>> { get<MutableSharedFlow<TdApi.UpdateFile>>() }
    single { MutableSharedFlow<TdApi.UpdateNewMessage>(extraBufferCapacity = 64) }
    single<SharedFlow<TdApi.UpdateNewMessage>> { get<MutableSharedFlow<TdApi.UpdateNewMessage>>() }
    single {
        // Wire TDLib client with basic update and exception handlers
        Client.create(
            { obj: Object? ->
                if (obj is TdApi.UpdateFile) {
                    get<MutableSharedFlow<TdApi.UpdateFile>>().tryEmit(obj)
                } else if (obj is TdApi.UpdateNewMessage) {
                    get<MutableSharedFlow<TdApi.UpdateNewMessage>>().tryEmit(obj)
                }
            },
            { _: Throwable? -> },
            { _: Throwable? -> }
        )
    }
    single<TelegramRepository> {
        TelegramRepositoryImpl(get(), get(), get(), get())
    }
}