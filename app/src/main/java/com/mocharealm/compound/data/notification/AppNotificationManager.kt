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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.mocharealm.compound.MainActivity
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
    private var me: TdApi.User? = null

    // 缓存每个对话的消息历史，用于构建 MessagingStyle
    private val messagesHistory = ConcurrentHashMap<Long, MutableList<NotificationCompat.MessagingStyle.Message>>()

    init {
        scope.launch {
            me = try { tdLibDataSource.send(TdApi.GetMe()) } catch (e: Exception) { null }
            createNotificationChannelGroup(me)
        }
        listenForUpdates()
        if (context is Application) {
            context.registerActivityLifecycleCallbacks(this)
        }
    }

    private fun createNotificationChannelGroup(me: TdApi.User?) {
        val groupId = "account_${me?.id ?: 0}"
        val groupName = if (me != null) "${me.firstName} ${me.lastName}".trim() else "Compound"
        
        notificationManager.createNotificationChannelGroup(
            android.app.NotificationChannelGroup(groupId, groupName)
        )
    }

    private fun getOrCreateChatChannel(chat: TdApi.Chat): String {
        val meId = me?.id ?: 0L
        val channelId = "chat_${meId}_${chat.id}"
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val name = chat.title
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                group = "account_$meId"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        return channelId
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
                    try { 
                        val bitmap = BitmapFactory.decodeFile(path)
                        bitmap?.let { getCircleBitmap(it) }
                    } catch (e: Exception) { null }
                }
            } else null
        }
    }

    private fun getCircleBitmap(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle((bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(), (bitmap.width / 2).toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)
        return output
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
        if (chat == null) return

        val channelId = getOrCreateChatChannel(chat)
        val isGroup = chat.type !is TdApi.ChatTypePrivate
        
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
            .setConversationTitle(if (isGroup) chat.title else null)
            .setGroupConversation(isGroup)

        history.forEach { messagingStyle.addMessage(it) }

        // 4. 构建 Action 和 Shortcut
        val finalNotificationId = (chatId and 0x7FFFFFFF).toInt()
        val shortcutId = "shortcut_$chatId"

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("compound://chat/$chatId")
            putExtra("chat_id", chatId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        // 如果是群组，Shortcut 图标应该用群组头像
        scope.launch {
            val chatIcon = if (isGroup) {
                chat.photo?.small?.let { loadBitmap(it) }?.let { IconCompat.createWithBitmap(it) }
            } else {
                senderIcon
            }

            // 发布 Shortcut 以支持 Conversations
            val shortcut = ShortcutInfoCompat.Builder(context, shortcutId)
                .setShortLabel(chat.title)
                .setIntent(contentIntent.setAction(Intent.ACTION_VIEW))
                .setPerson(person)
                .setIcon(chatIcon ?: IconCompat.createWithResource(context, android.R.drawable.ic_dialog_email))
                .setLongLived(true)
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)

            val contentPendingIntent = PendingIntent.getActivity(
                context, finalNotificationId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 5. 构建通知
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setLargeIcon(chatIcon?.toIcon(context)?.let { null } ?: chatIcon?.let { 
                    // 这里直接用 Bitmap
                    chat.photo?.small?.let { loadBitmap(it) }
                })
                .setStyle(messagingStyle)
                .setContentIntent(contentPendingIntent)
                .setShortcutId(shortcutId)
                .setLocusId(androidx.core.content.LocusIdCompat(shortcutId))
                .setWhen(date.toLong() * 1000)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addPerson(person)

            notificationManager.notify(finalNotificationId, builder.build())
        }
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
