package com.mocharealm.compound.data.notification

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentHashMap

class AppNotificationManager(
    private val context: Context,
    private val tdLibDataSource: TdLibDataSource
) : Application.ActivityLifecycleCallbacks {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // 追踪前台 Activity 正在查看的对话
    private var currentChatId: Long? = null

    // 缓存每个对话的消息历史，用于构建 MessagingStyle
    private val messagesHistory = ConcurrentHashMap<Long, MutableList<NotificationCompat.MessagingStyle.Message>>()

    init {
        createNotificationChannel()
        listenForUpdates()
        if (context is Application) {
            context.registerActivityLifecycleCallbacks(this)
        }
    }

    private fun createNotificationChannel() {
        val name = "Chat Notifications"
        val descriptionText = "Notifications for new messages"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun listenForUpdates() {
        Log.d("AppNotificationManager", "Starting to listen for updates...")
        scope.launch {
            tdLibDataSource.updates.collect { update ->
                when (update) {
                    is TdApi.UpdateNotification -> {
                        handleNotificationUpdate(update.notificationGroupId, update.notification)
                    }
                    is TdApi.UpdateNotificationGroup -> {
                        update.addedNotifications.forEach { notification ->
                            handleNotificationUpdate(update.notificationGroupId, notification)
                        }
                        update.removedNotificationIds.forEach { notificationId ->
                            notificationManager.cancel(notificationId)
                        }
                    }
                    is TdApi.UpdateActiveNotifications -> {
                        update.groups.forEach { group ->
                            group.notifications.forEach { notification ->
                                handleNotificationUpdate(group.id, notification)
                            }
                        }
                    }
                    is TdApi.UpdateChatReadInbox -> {
                        messagesHistory.remove(update.chatId)
                        notificationManager.cancel((update.chatId and 0x7FFFFFFF).toInt())
                    }
                }
            }
        }
    }

    private fun handleNotificationUpdate(groupId: Int, notification: TdApi.Notification) {
        scope.launch {
            val (chatId, senderId, contentText, date) = when (val info = notification.type) {
                is TdApi.NotificationTypeNewMessage -> {
                    val msg = info.message
                    Four(msg.chatId, msg.senderId, getMessageText(msg.content), msg.date)
                }
                is TdApi.NotificationTypeNewPushMessage -> {
                    val text = if (info.content is TdApi.PushMessageContentText) {
                        (info.content as TdApi.PushMessageContentText).text
                    } else {
                        "New message"
                    }
                    Four(0L, null, text, notification.date)
                }
                else -> return@launch
            }

            if (chatId != 0L && chatId == currentChatId) return@launch

            val chat = if (chatId != 0L) {
                try { tdLibDataSource.send(TdApi.GetChat(chatId)) } catch (e: Exception) { null }
            } else null

            // 获取发送者姓名和头像
            val (senderName, senderIcon) = if (senderId != null) {
                resolveSenderInfo(senderId)
            } else {
                Pair("User", null)
            }

            showTgxStyledNotification(chat, senderName, senderIcon, contentText, date, notification.id)
        }
    }

    private suspend fun resolveSenderInfo(senderId: TdApi.MessageSender): Pair<String, IconCompat?> {
        return when (senderId) {
            is TdApi.MessageSenderUser -> {
                val user = try { tdLibDataSource.send(TdApi.GetUser(senderId.userId)) } catch (e: Exception) { null }
                val name = if (user != null) "${user.firstName} ${user.lastName}".trim() else "User"
                val avatar = user?.profilePhoto?.small?.let { loadBitmap(it) }
                Pair(name, avatar?.let { IconCompat.createWithBitmap(it) })
            }
            is TdApi.MessageSenderChat -> {
                val chat = try { tdLibDataSource.send(TdApi.GetChat(senderId.chatId)) } catch (e: Exception) { null }
                val name = chat?.title ?: "Chat"
                val avatar = chat?.photo?.small?.let { loadBitmap(it) }
                Pair(name, avatar?.let { IconCompat.createWithBitmap(it) })
            }
            else -> Pair("Unknown", null)
        }
    }

    private suspend fun loadBitmap(file: TdApi.File): Bitmap? {
        // 如果文件还没下载，尝试下载
        val downloadedFile = if (!file.local.isDownloadingCompleted) {
            try {
                tdLibDataSource.send(TdApi.DownloadFile(file.id, 1, 0, 0, true))
            } catch (e: Exception) { null }
        } else file

        return downloadedFile?.local?.path?.let { path ->
            if (path.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
                }
            } else null
        }
    }

    private fun getMessageText(content: TdApi.MessageContent): String {
        return when (content) {
            is TdApi.MessageText -> content.text.text
            is TdApi.MessagePhoto -> "📷 Photo"
            is TdApi.MessageVideo -> "📹 Video"
            is TdApi.MessageSticker -> "${content.sticker.emoji} Sticker"
            is TdApi.MessageAnimation -> "🎬 GIF"
            is TdApi.MessageAudio -> "🎵 Audio"
            is TdApi.MessageVoiceNote -> "🎤 Voice Message"
            else -> "💬 Message"
        }
    }

    private fun showTgxStyledNotification(chat: TdApi.Chat?, senderName: String, senderIcon: IconCompat?, text: String, date: Int, notificationId: Int) {
        val chatId = chat?.id ?: 0L
        val isGroup = chat?.let { it.type !is TdApi.ChatTypePrivate } ?: false
        
        // 1. 构建 Person (发送者)
        val person = Person.Builder()
            .setName(senderName)
            .setIcon(senderIcon)
            .setKey(senderName)
            .build()

        // 2. 构建消息历史 (MessagingStyle)
        val styleMessage = NotificationCompat.MessagingStyle.Message(
            text,
            date.toLong() * 1000,
            person
        )

        val history = messagesHistory.getOrPut(chatId) { mutableListOf() }
        history.add(styleMessage)
        if (history.size > 10) history.removeAt(0)

        // 3. 创建样式
        val myself = Person.Builder().setName("Me").build()
        val messagingStyle = NotificationCompat.MessagingStyle(myself)
            .setConversationTitle(if (isGroup) chat?.title else null)
            .setGroupConversation(isGroup)

        history.forEach { messagingStyle.addMessage(it) }

        // 4. 构建 Action (已读和回复)
        val finalNotificationId = if (chatId != 0L) (chatId and 0x7FFFFFFF).toInt() else notificationId
        
        val readIntent = Intent(context, NotificationReadReceiver::class.java).apply {
            putExtra("chat_id", chatId)
        }
        val readPendingIntent = PendingIntent.getBroadcast(
            context, finalNotificationId, readIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val readAction = NotificationCompat.Action.Builder(0, "Mark as Read", readPendingIntent).build()

        val remoteInput = RemoteInput.Builder(NotificationReplyReceiver.KEY_TEXT_REPLY)
            .setLabel("Reply...")
            .build()
        val replyIntent = Intent(context, NotificationReplyReceiver::class.java).apply {
            putExtra("chat_id", chatId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context, finalNotificationId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val replyAction = NotificationCompat.Action.Builder(0, "Reply", replyPendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        // 5. 构建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setStyle(messagingStyle)
            .setWhen(date.toLong() * 1000)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(replyAction)
            .addAction(readAction)

        notificationManager.notify(finalNotificationId, builder.build())
    }

    private data class Four<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    // --- ActivityLifecycleCallbacks ---

    override fun onActivityResumed(activity: Activity) {
        val chatId = activity.intent?.getLongExtra("chat_id", 0L)
        if (chatId != null && chatId != 0L) {
            currentChatId = chatId
            notificationManager.cancel((chatId and 0x7FFFFFFF).toInt())
            messagesHistory.remove(chatId)
            scope.launch { tdLibDataSource.send(TdApi.OpenChat(chatId)) }
        }
    }

    override fun onActivityPaused(activity: Activity) {
        currentChatId?.let { scope.launch { tdLibDataSource.send(TdApi.CloseChat(it)) } }
        currentChatId = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    companion object {
        private const val CHANNEL_ID = "telegram_notifications"
    }
}
