package com.mocharealm.compound.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatMessageTime(): String {
    if (this == 0L) return ""
    val date = Date(this * 1000)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}