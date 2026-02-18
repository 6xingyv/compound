package com.mocharealm.tci18n.core

/**
 * Represents a localized string value from TDLib.
 * Can be a simple ordinary string or a complex pluralized object.
 */
sealed class TdStringValue {
    data class Ordinary(val value: String) : TdStringValue()

    data class Pluralized(
        val zero: String,
        val one: String,
        val two: String,
        val few: String,
        val many: String,
        val other: String
    ) : TdStringValue()

    /**
     * Get the string value for the given count (for plurals) or the ordinary value.
     */
    fun getString(count: Int? = null): String {
        return when (this) {
            is Ordinary -> value
            is Pluralized -> {
                if (count == null) other else selectPlural(count)
            }
        }
    }

    private fun Pluralized.selectPlural(count: Int): String {
        // Basic selection logic. 
        // Note: Real Android PluralRules would be better but this is a good cross-platform-ish start.
        // We can improve this using android.icu.text.PluralRules in the app module if needed.
        return when (count) {
            0 -> zero.takeIf { it.isNotEmpty() } ?: other
            1 -> one.takeIf { it.isNotEmpty() } ?: other
            2 -> two.takeIf { it.isNotEmpty() } ?: other
            else -> other // Simplified for now
        }
    }

    companion object {
        val Empty = Ordinary("")
    }
}
