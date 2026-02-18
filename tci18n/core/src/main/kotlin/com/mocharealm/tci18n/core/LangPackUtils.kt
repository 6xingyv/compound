package com.mocharealm.tci18n.core

import android.util.Log
import java.util.Locale

/**
 * Maps a Java [Locale] to a TDLib language pack identifier.
 *
 * TDLib uses lowercase-hyphen IDs like "en", "zh-hans", "zh-hant", "pt-br".
 * Java's [Locale.getLanguage] only returns "zh" for Chinese, losing the
 * Simplified / Traditional distinction, so we consult [Locale.getScript]
 * and [Locale.getCountry] to produce the correct ID.
 */
fun tdLangPackId(locale: Locale): String {
    val lang = locale.language
    val script = locale.script
    val country = locale.country
    Log.e("Inr","lang:$lang \nscript:$script \ncountry:$country")

    return when {
        script.isNotEmpty() -> "$lang-${script.lowercase()}"
        lang == "zh" -> when (country) {
            "TW", "HK", "MO" -> "zh-hant-beta"
            else -> "zh-hans-beta"
        }
        // Portuguese: pt-BR vs pt
        lang == "pt" -> when(country) {
            "BR"-> "pt-br"
            else -> "pt-pt"
        }
        // Default: use simple language code
        else -> lang
    }
}