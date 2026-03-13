package com.mocharealm.compound.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue


class NotificationReadReceiver : BroadcastReceiver(), KoinComponent {

    private val tdLibDataSource: TdLibDataSource by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getLongExtra("chat_id", 0L)
        if (chatId == 0L) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 获取对话详情以找到最后一条消息 ID
                val chat = tdLibDataSource.send(TdApi.GetChat(chatId))
                chat.lastMessage?.let { lastMessage ->
                    // 2. 将最后一条消息标记为已读，TDLib 会自动同步该对话的所有消息为已读
                    tdLibDataSource.send(TdApi.ViewMessages(chatId, longArrayOf(lastMessage.id), null, true))
                    Log.d("NotificationReadReceiver", "Marked chat $chatId as read")
                }
            } catch (e: Exception) {
                Log.e("NotificationReadReceiver", "Failed to mark chat as read", e)
            }
        }
    }
}