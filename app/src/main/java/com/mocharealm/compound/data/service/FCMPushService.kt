package com.mocharealm.compound.data.service

import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mocharealm.compound.domain.model.AuthState
import com.mocharealm.compound.domain.repository.TelegramRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FCMPushService : FirebaseMessagingService(), KoinComponent {

    private val telegramRepository: TelegramRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMPushService"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        scope.launch {
            if (telegramRepository.getAuthenticationState() is AuthState.Ready) {
                telegramRepository.registerDevice(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        Log.d(TAG, "FCM message received, data: $data")

        if (data.isNotEmpty()) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "compound:FCMPushService"
            )

            try {
                wakeLock.acquire(10 * 1000L /*10 seconds*/)
                
                // Construct JSON from the entire data map, as TDLib expects
                val json = JSONObject()
                for ((key, value) in data) {
                    json.put(key, value)
                }
                val jsonPayload = json.toString()
                Log.d(TAG, "Processing push notification with payload: $jsonPayload")

                runBlocking {
                    Log.d(TAG, "Sending ProcessPushNotification to TDLib...")
                    telegramRepository.processPushNotification(jsonPayload)
                    Log.d(TAG, "ProcessPushNotification sent. Waiting for TDLib to sync and emit notifications...")
                    // Give TDLib more time to initialize, connect, and emit UpdateNotification when cold-started.
                    // Telegram-X often uses a 5 to 15 second timeout for this.
                    delay(8000)
                    Log.d(TAG, "Finished waiting for TDLib sync.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing push notification", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No need to cancel scope here as Service might be killed and recreated, 
        // but it's good practice. However, FirebaseMessagingService has its own lifecycle.
    }
}
