package com.mocharealm.compound.ui.util

import android.icu.text.DateFormat
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.Calendar
import java.util.Locale

fun Long.formatMessageTimestamp(): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = this@formatMessageTimestamp * 1000L}
    val locale = Locale.getDefault()

    if (isSameDay(now, msgTime)) {
        return DateFormat.getInstanceForSkeleton("Hm", locale).format(msgTime)
    }

    if (isYesterday(now, msgTime)) {
        val timeStr = DateFormat.getInstanceForSkeleton("Hm", locale).format(msgTime)
        val relative = RelativeDateTimeFormatter.getInstance(locale)
            .format(
                RelativeDateTimeFormatter.Direction.LAST,
                RelativeDateTimeFormatter.AbsoluteUnit.DAY
            )
        return "$relative $timeStr"
    }

    if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)) {
        return DateFormat.getInstanceForSkeleton("MMMdHm", locale).format(msgTime)
    }

    return DateFormat.getInstanceForSkeleton("yMMMdHm", locale).format(msgTime)
}

fun isSameDay(now: Calendar, msgTime: Calendar): Boolean {
    return now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
}

fun isYesterday(now: Calendar, msgTime: Calendar): Boolean {
    // 创建一个“昨天”的副本进行比对
    val yesterday = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, msgTime)
}

fun isSameYear(now: Calendar, msgTime: Calendar): Boolean {
    return now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
}

// 判断是否在最近一周内（微信通常在 2-7 天显示“星期几”）
fun isWithinWeek(now: Calendar, msgTime: Calendar): Boolean {
    val weekAgo = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -7)
    }
    return msgTime.after(weekAgo) && !isSameDay(now, msgTime)
}