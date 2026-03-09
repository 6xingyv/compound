package com.mocharealm.compound.data.source.local

import android.content.Context

class ChatLocalDataSource(private val context: Context) {
    private val prefs = context.getSharedPreferences("chat_read_positions", Context.MODE_PRIVATE)

    fun saveReadPosition(chatId: Long, messageId: Long) {
        prefs.edit().putLong("chat_$chatId", messageId).apply()
    }

    fun getReadPosition(chatId: Long): Long {
        return prefs.getLong("chat_$chatId", 0L)
    }
}
