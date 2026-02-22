package com.mocharealm.compound.domain.usecase

import com.mocharealm.compound.domain.repository.PhoneFormatRepository
import com.mocharealm.compound.domain.util.DefaultPhoneNumberSanitizer

class ValidatePhoneNumberUseCase(
    private val repository: PhoneFormatRepository,
) {
    private val sanitizer = DefaultPhoneNumberSanitizer

    operator fun invoke(phoneNumber: String, defaultCountryCode: String? = null): Boolean {
        val cleanNumber = sanitizer.sanitize(phoneNumber)

        return if (cleanNumber.startsWith("+")) {
            validateInternational(cleanNumber.substring(1))
        } else {
            validateLocal(cleanNumber, defaultCountryCode)
        }
    }

    private fun validateInternational(number: String): Boolean {
        for (i in 1..minOf(3, number.length)) {
            val callingCode = number.substring(0, i)
            val info = repository.getCallingCodeInfo(callingCode)
            if (info != null) {
                return info.isValid(number.substring(i))
            }
        }
        return false
    }

    private fun validateLocal(number: String, defaultCountryCode: String?): Boolean {
        val defaultCallingCode = if (!defaultCountryCode.isNullOrEmpty()) {
            repository.getCallingCodeForCountry(defaultCountryCode)
        } else {
            repository.getDefaultCallingCode()
        }

        val info = defaultCallingCode?.let { repository.getCallingCodeInfo(it) }
        return info?.isValid(number) ?: false
    }
}