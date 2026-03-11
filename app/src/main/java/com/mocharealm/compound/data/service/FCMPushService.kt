package com.mocharealm.compound.data.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FCMPushService : FirebaseMessagingService(), KoinComponent {

    private val telegramRepository: TelegramRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            if (telegramRepository.getAuthenticationState() is AuthState.Ready) {
                telegramRepository.registerDevice(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val json = remoteMessage.data["json"]
        if (json != null) {
            // Use runBlocking to ensure processing finishes before the service potentially stops
            runBlocking {
                telegramRepository.processPushNotification(json)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No need to cancel scope here as Service might be killed and recreated, 
        // but it's good practice. However, FirebaseMessagingService has its own lifecycle.
    }
}
