package com.mocharealm.compound.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.mocharealm.compound.R
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi

class AppNotificationManager(
    private val context: Context,
    private val tdLibDataSource: TdLibDataSource
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        createNotificationChannel()
        listenForUpdates()
    }

    private fun createNotificationChannel() {
        val name = "Chat Notifications"
        val descriptionText = "Notifications for new messages"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun listenForUpdates() {
        scope.launch {
            tdLibDataSource.updates.filterIsInstance<TdApi.UpdateNotification>().collect { update ->
                showNotification(update.notificationGroupId, update.notification)
            }
        }

        scope.launch {
            tdLibDataSource.updates.filterIsInstance<TdApi.UpdateNotificationGroup>().collect { update ->
                // Handle notification group updates (e.g., summary notifications)
                // update.addedNotifications contains new notifications in this group
                update.addedNotifications.forEach { notification ->
                    showNotification(update.notificationGroupId, notification)
                }
                update.removedNotificationIds.forEach { notificationId ->
                    notificationManager.cancel(notificationId)
                }
            }
        }

        scope.launch {
            tdLibDataSource.updates.filterIsInstance<TdApi.UpdateActiveNotifications>().collect { update ->
                // Sync active notifications if needed
                update.groups.forEach { group ->
                    group.notifications.forEach { notification ->
                        showNotification(group.id, notification)
                    }
                }
            }
        }

        scope.launch {
            tdLibDataSource.updates.filterIsInstance<TdApi.RemoveNotification>().collect { update ->
                notificationManager.cancel(update.notificationId)
            }
        }

        scope.launch {
            tdLibDataSource.updates.filterIsInstance<TdApi.RemoveNotificationGroup>().collect { update ->
            }
        }
    }

    private fun showNotification(groupId: Int, notification: TdApi.Notification) {
        val type = notification.type

        // TODO: 这里可以根据 type (TdApi.NotificationType) 来定制不同类型的样式
        // 例如：TdApi.NotificationTypeNewMessage, TdApi.NotificationTypeNewPushMessage 等

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // TODO: 修改通知图标
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        when (type) {
            is TdApi.NotificationTypeNewMessage -> {
                val message = type.message
                // TODO: 修改消息通知的标题和内容显示方式
                builder.setContentTitle("New Message")
                // builder.setContentText(message.content.toString()) // 需要更复杂的解析
            }
            is TdApi.NotificationTypeNewPushMessage -> {
                // TODO: 修改推送消息的标题和内容显示方式
                builder.setContentTitle(type.senderName)
                // builder.setContentText(type.content.toString())
            }
        }

        // TODO: 在这里添加更多自定义样式，例如：
        // .setStyle(NotificationCompat.MessagingStyle(user))
        // .setLargeIcon(avatarBitmap)
        // .addAction(...)

        notificationManager.notify(notification.id, builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "telegram_notifications"
    }
}
