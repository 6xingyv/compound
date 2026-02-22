package com.mocharealm.compound.domain.util

interface PhoneNumberSanitizer {
    fun sanitize(phoneNumber: String, keepPlus: Boolean = true): String
    fun sanitizeStrict(phoneNumber: String): String
}

object DefaultPhoneNumberSanitizer : PhoneNumberSanitizer {
    override fun sanitize(phoneNumber: String, keepPlus: Boolean): String {
        val validChars = buildString {
            append("0123456789")
            if (keepPlus) append("+")
            append("*#")
        }

        return phoneNumber.filter { it in validChars }
    }

    override fun sanitizeStrict(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }
}


