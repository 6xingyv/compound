package com.mocharealm.compound.domain.repository

import com.mocharealm.compound.domain.model.CallingCodeInfo

interface PhoneFormatRepository {
    fun getCallingCodeInfo(callingCode: String): CallingCodeInfo?
    fun getCallingCodeForCountry(countryCode: String): String?
    fun getCountriesForCallingCode(callingCode: String): List<String>?
    fun getDefaultCallingCode(): String?
    fun getAllCountryCallingCodes(): Map<String, String>
}