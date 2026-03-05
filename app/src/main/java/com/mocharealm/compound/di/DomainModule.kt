package com.mocharealm.compound.di

import com.mocharealm.compound.domain.usecase.auth.CheckAuthenticationCodeUseCase
import com.mocharealm.compound.domain.usecase.auth.CheckAuthenticationPasswordUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileUseCase
import com.mocharealm.compound.domain.usecase.CloseChatUseCase
import com.mocharealm.compound.domain.usecase.DownloadFileWithProgressUseCase
import com.mocharealm.compound.domain.usecase.FormatPersonNameUseCase
import com.mocharealm.compound.domain.usecase.phonenumber.FormatPhoneNumberUseCase
import com.mocharealm.compound.domain.usecase.auth.AwaitAuthenticationStateUseCase
import com.mocharealm.compound.domain.usecase.auth.GetAuthenticationStateUseCase
import com.mocharealm.compound.domain.usecase.GetChatMessagesUseCase
import com.mocharealm.compound.domain.usecase.GetChatUseCase
import com.mocharealm.compound.domain.usecase.GetChatsUseCase
import com.mocharealm.compound.domain.usecase.GetInstalledStickerSetsUseCase
import com.mocharealm.compound.domain.usecase.GetStickerSetStickersUseCase
import com.mocharealm.compound.domain.usecase.OpenChatUseCase
import com.mocharealm.compound.domain.usecase.GetCurrentUserUseCase
import com.mocharealm.compound.domain.usecase.GetInternalLinkUseCase
import com.mocharealm.compound.domain.usecase.LogoutUseCase
import com.mocharealm.compound.domain.usecase.SendFilesUseCase
import com.mocharealm.compound.domain.usecase.SendLocationUseCase
import com.mocharealm.compound.domain.usecase.SendMessageUseCase
import com.mocharealm.compound.domain.usecase.SendStickerUseCase
import com.mocharealm.compound.domain.usecase.SetAuthenticationPhoneNumberUseCase
import com.mocharealm.compound.domain.usecase.SubscribeToMessageUpdatesUseCase
import com.mocharealm.compound.domain.usecase.phonenumber.ValidatePhoneNumberUseCase
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
        AwaitAuthenticationStateUseCase(get())
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
    factory {
        FormatPersonNameUseCase(get())
    }
    factory {
        GetInternalLinkUseCase(get())
    }
    factory {
        OpenChatUseCase(get())
    }
    factory {
        CloseChatUseCase(get())
    }
    factory {
        GetInstalledStickerSetsUseCase(get())
    }
    factory {
        GetStickerSetStickersUseCase(get())
    }
    factory {
        SendStickerUseCase(get())
    }
    factory {
        SendLocationUseCase(get())
    }
    factory {
        SendFilesUseCase(get())
    }
}