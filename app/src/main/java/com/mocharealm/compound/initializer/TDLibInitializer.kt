package com.mocharealm.compound.initializer

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.messaging.FirebaseMessaging
import com.mocharealm.compound.data.notification.AppNotificationManager
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TDLibInitializer : Initializer<Unit>, KoinComponent {
    private val telegramRepository: TelegramRepository by inject()
    private val notificationManager: AppNotificationManager by inject()

    override fun create(context: Context) {
        // Trigger instantiation of notification manager to start listening for updates
        notificationManager
        
        // Trigger instantiation of TelegramRepository (which calls Client.create and SetTdlibParameters)
        telegramRepository

        // Register current FCM token if available AND logged in
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (token != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (telegramRepository.getAuthenticationState() is AuthState.Ready) {
                            telegramRepository.registerDevice(token)
                        }
                    }
                }
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(KoinInitializer::class.java)
    }
}
