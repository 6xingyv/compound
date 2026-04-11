package com.mocharealm.compound.di

import com.mocharealm.compound.domain.repository.*
import com.mocharealm.compound.mock.MockAuthRepository
import com.mocharealm.compound.mock.MockChatRepository
import com.mocharealm.compound.mock.MockMessageRepository
import com.mocharealm.tci18n.core.TdStringProvider
import com.mocharealm.tci18n.core.TdStringValue
import org.koin.dsl.module

val mockModule = module {
    single<AuthRepository> { MockAuthRepository() }
    single<ChatRepository> { MockChatRepository() }
    single<MessageRepository> { MockMessageRepository() }
    
    // Mock TdStringProvider to avoid real TDLib calls for strings
    single<TdStringProvider> {
        TdStringProvider { keys ->
            keys.associateWith { TdStringValue.Ordinary(it) }
        }
    }
}
