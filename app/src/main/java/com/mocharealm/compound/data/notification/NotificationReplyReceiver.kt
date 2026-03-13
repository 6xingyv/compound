package com.mocharealm.compound.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.mocharealm.compound.data.source.remote.TdLibDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class NotificationReplyReceiver : BroadcastReceiver(), KoinComponent {

    private val tdLibDataSource: TdLibDataSource by inject()

    companion object {
        const val KEY_TEXT_REPLY = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getLongExtra("chat_id", 0L)
        if (chatId == 0L) return

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()
        if (replyText.isNullOrBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. 发送正在输入状态
                // 使用构造函数：SendChatAction(long chatId, MessageTopic topicId, String businessConnectionId, ChatAction action)
                tdLibDataSource.send(TdApi.SendChatAction(chatId, null, null, TdApi.ChatActionTyping()))

                // 2. 构建消息内容
                val inputMessageContent = TdApi.InputMessageText(
                    TdApi.FormattedText(replyText, emptyArray()),
                    null,
                    true
                )

                // 3. 发送消息
                // 使用构造函数：SendMessage(long chatId, MessageTopic topicId, InputMessageReplyTo replyTo, MessageSendOptions options, ReplyMarkup replyMarkup, InputMessageContent inputMessageContent)
                tdLibDataSource.send(
                    TdApi.SendMessage(
                    chatId,
                    null, // topicId
                    null, // replyTo
                    null, // options
                    null, // replyMarkup
                    inputMessageContent
                ))

                // 4. 回复成功后自动已读
                val chat = tdLibDataSource.send(TdApi.GetChat(chatId))
                chat.lastMessage?.let {
                    tdLibDataSource.send(TdApi.ViewMessages(chatId, longArrayOf(it.id), null, true))
                }

                Log.d("NotificationReplyReceiver", "Reply sent to chat $chatId")
            } catch (e: Exception) {
                Log.e("NotificationReplyReceiver", "Failed to send reply", e)
            }
        }
    }
}