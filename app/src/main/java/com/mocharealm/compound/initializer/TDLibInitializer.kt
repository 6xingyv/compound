package com.mocharealm.compound.initializer

import android.content.Context
import androidx.startup.Initializer
import com.mocharealm.compound.domain.repository.TelegramRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TDLibInitializer : Initializer<Unit>, KoinComponent {
    private val telegramRepository: TelegramRepository by inject()

    override fun create(context: Context) {
        // Trigger instantiation of TelegramRepository (which calls Client.create and SetTdlibParameters)
        telegramRepository
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(KoinInitializer::class.java)
    }
}
