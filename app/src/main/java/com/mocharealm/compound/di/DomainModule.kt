package com.mocharealm.compound.di

import com.mocharealm.compound.domain.usecase.CheckAuthenticationCodeUseCase
import com.mocharealm.compound.domain.usecase.CheckAuthenticationPasswordUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileWithProgressUseCase
import com.mocharealm.compound.domain.usecase.FormatPhoneNumberUseCase
import com.mocharealm.compound.domain.usecase.GetAuthenticationStateUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.GetCurrentUserUseCase
import com.mocharealm.compound.domain.usecase.LogoutUseCase
import com.mocharealm.compound.domain.usecase.SendFilesUseCase
import com.mocharealm.compound.domain.usecase.SendMessageUseCase
import com.mocharealm.compound.domain.usecase.SetAuthenticationPhoneNumberUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import com.mocharealm.compound.domain.usecase.ValidatePhoneNumberUseCase
import org.koin.dsl.module

val domainModule = module {
    factory {
        SetAuthenticationPhoneNumberUseCase(get())
    }
    factory {
        CheckAuthenticationCodeUseCase(get())
    }
    factory {
        CheckAuthenticationPasswordUseCase(get())
    }
    factory {
        GetCurrentUserUseCase(get())
    }
    factory {
        LogoutUseCase(get())
    }
    factory {
        GetAuthenticationStateUseCase(get())
    }
    factory {
        GetChatsUseCase(get())
    }
    factory {
        GetChatMessagesUseCase(get())
    }
    factory {
        DownloadFileUseCase(get())
    }
    factory {
        SendMessageUseCase(get())
    }
    factory {
        SendFilesUseCase(get())
    }
    factory {
        GetChatUseCase(get())
    }
    factory {
        SubscribeToMessageUpdatesUseCase(get())
    }
    factory {
        DownloadFileWithProgressUseCase(get())
    }
    factory {
        FormatPhoneNumberUseCase(get())
    }
    factory {
        ValidatePhoneNumberUseCase(get())
    }
}