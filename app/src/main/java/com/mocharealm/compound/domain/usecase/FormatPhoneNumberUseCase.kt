package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.PhoneFormatRepository
import com.mocharealm.compound.domain.util.DefaultPhoneNumberSanitizer

class FormatPhoneNumberUseCase(
    private val repository: PhoneFormatRepository,
) {
    private val sanitizer = DefaultPhoneNumberSanitizer
    operator fun invoke(phoneNumber: String, defaultCountryCode: String? = null): String {
        val cleanNumber = sanitizer.sanitize(phoneNumber)

        return if (cleanNumber.startsWith("+")) {
            formatInternational(cleanNumber.substring(1))
        } else {
            formatLocal(cleanNumber, defaultCountryCode)
        }
    }

    private fun formatInternational(number: String): String {
        for (i in 1..minOf(3, number.length)) {
            val callingCode = number.substring(0, i)
            val info = repository.getCallingCodeInfo(callingCode)
            if (info != null) {
                val formatted = info.format(number.substring(i))
                return "+$callingCode$formatted"
            }
        }
        return "+$number"
    }

    private fun formatLocal(number: String, defaultCountryCode: String?): String {
        val defaultCallingCode = if (!defaultCountryCode.isNullOrEmpty()) {
            repository.getCallingCodeForCountry(defaultCountryCode)
        } else {
            repository.getDefaultCallingCode()
        }

        val info = defaultCallingCode?.let { repository.getCallingCodeInfo(it) }
        return info?.format(number) ?: number
    }
}