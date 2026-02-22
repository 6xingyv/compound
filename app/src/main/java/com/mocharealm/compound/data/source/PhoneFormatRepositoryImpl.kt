package com.mocharealm.compound.data.source

import android.content.Context
import com.mocharealm.compound.domain.model.CallingCodeInfo
import com.mocharealm.compound.domain.model.PhoneRule
import com.mocharealm.compound.domain.model.RuleSet
import com.mocharealm.compound.domain.repository.PhoneFormatRepository
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PhoneFormatRepositoryImpl(
    private val context: Context,
    private val defaultCountryCode: String? = null
) : PhoneFormatRepository {

    private var data: ByteArray? = null
    private var buffer: ByteBuffer? = null

    private val callingCodeOffsets = mutableMapOf<String, Int>()
    private val callingCodeCountries = mutableMapOf<String, MutableList<String>>()
    private val countryCallingCode = mutableMapOf<String, String>()
    private var defaultCallingCode: String? = null

    private val callingCodeInfoCache = mutableMapOf<String, CallingCodeInfo>()

    init {
        loadDataFile()
    }

    private fun loadDataFile() {
        try {
            context.assets.open("configs/phone_formats.dat").use { inputStream ->
                ByteArrayOutputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                    data = outputStream.toByteArray()
                }
            }

            data?.let {
                buffer = ByteBuffer.wrap(it).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                }
                parseDataHeader()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseDataHeader() {
        val count = value32(0) ?: return
        val base = count * 12 + 4
        var spot = 4

        repeat(count) {
            val callingCode = valueString(spot) ?: return@repeat
            spot += 4
            val country = valueString(spot) ?: return@repeat
            spot += 4
            val offset = (value32(spot) ?: 0) + base
            spot += 4

            if (country == (defaultCountryCode ?: getDefaultLocaleCountry())) {
                defaultCallingCode = callingCode
            }

            countryCallingCode[country] = callingCode
            callingCodeOffsets[callingCode] = offset

            val countries = callingCodeCountries.getOrPut(callingCode) { mutableListOf() }
            countries.add(country)
        }
    }

    private fun loadCallingCodeInfo(callingCode: String): CallingCodeInfo? {
        val offset = callingCodeOffsets[callingCode] ?: return null
        val data = data ?: return null
        val buffer = buffer ?: return null

        var currentOffset = offset
        val start = currentOffset

        // 读取头部信息
        val block1Len = value16(currentOffset) ?: 0
        currentOffset += 2
        currentOffset += 2 // 跳过保留字段
        val block2Len = value16(currentOffset) ?: 0
        currentOffset += 2
        currentOffset += 2 // 跳过保留字段
        val setCnt = value16(currentOffset) ?: 0
        currentOffset += 2
        currentOffset += 2 // 跳过保留字段

        // 读取trunk prefixes
        val trunkPrefixes = mutableListOf<String>()
        while (true) {
            val str = valueString(currentOffset) ?: break
            if (str.isEmpty()) break
            trunkPrefixes.add(str)
            currentOffset += str.length + 1
        }
        currentOffset++ // 跳过结束符

        // 读取intl prefixes
        val intlPrefixes = mutableListOf<String>()
        while (true) {
            val str = valueString(currentOffset) ?: break
            if (str.isEmpty()) break
            intlPrefixes.add(str)
            currentOffset += str.length + 1
        }
        currentOffset++ // 跳过结束符

        // 读取规则集
        val ruleSets = mutableListOf<RuleSet>()
        currentOffset = start + block1Len

        repeat(setCnt) {
            val matchLen = value16(currentOffset) ?: 0
            currentOffset += 2
            val ruleCnt = value16(currentOffset) ?: 0
            currentOffset += 2

            val rules = mutableListOf<PhoneRule>()
            var hasRuleWithIntlPrefix = false
            var hasRuleWithTrunkPrefix = false

            repeat(ruleCnt) {
                val minVal = value32(currentOffset) ?: 0
                currentOffset += 4
                val maxVal = value32(currentOffset) ?: 0
                currentOffset += 4
                val byte8 = data[currentOffset++].toInt()
                val maxLen = data[currentOffset++].toInt()
                val otherFlag = data[currentOffset++].toInt()
                val prefixLen = data[currentOffset++].toInt()
                val flag12 = data[currentOffset++].toInt()
                val flag13 = data[currentOffset++].toInt()

                val strOffset = value16(currentOffset) ?: 0
                currentOffset += 2

                var format = valueString(start + block1Len + block2Len + strOffset) ?: ""

                // 处理[[...]]占位符
                format = format.replace("\\[\\[(.*?)\\]\\]".toRegex(), "")

                val rule = PhoneRule(
                    minVal = minVal,
                    maxVal = maxVal,
                    byte8 = byte8,
                    maxLen = maxLen,
                    otherFlag = otherFlag,
                    prefixLen = prefixLen,
                    flag12 = flag12,
                    flag13 = flag13,
                    format = format
                )

                rules.add(rule)

                if (rule.hasIntlPrefix) hasRuleWithIntlPrefix = true
                if ((otherFlag and 0x02) != 0) hasRuleWithTrunkPrefix = true
            }

            ruleSets.add(
                RuleSet(
                    matchLen = matchLen,
                    rules = rules,
                    hasRuleWithIntlPrefix = hasRuleWithIntlPrefix,
                    hasRuleWithTrunkPrefix = hasRuleWithTrunkPrefix
                )
            )
        }

        return CallingCodeInfo(
            callingCode = callingCode,
            countries = callingCodeCountries[callingCode] ?: emptyList(),
            trunkPrefixes = trunkPrefixes,
            intlPrefixes = intlPrefixes,
            ruleSets = ruleSets
        )
    }

    private fun getDefaultLocaleCountry(): String {
        return java.util.Locale.getDefault().country.lowercase()
    }

    private fun value32(offset: Int): Int? {
        val buffer = buffer ?: return null
        if (offset + 4 <= (data?.size ?: 0)) {
            buffer.position(offset)
            return buffer.int
        }
        return null
    }

    private fun value16(offset: Int): Int? {
        val buffer = buffer ?: return null
        if (offset + 2 <= (data?.size ?: 0)) {
            buffer.position(offset)
            return buffer.short.toInt()
        }
        return null
    }

    private fun valueString(offset: Int): String? {
        val data = data ?: return null
        var end = offset
        while (end < data.size && data[end] != 0.toByte()) {
            end++
        }
        return if (end > offset) {
            String(data, offset, end - offset)
        } else {
            ""
        }
    }

    // PhoneFormatRepository 接口实现
    override fun getCallingCodeInfo(callingCode: String): CallingCodeInfo? {
        return callingCodeInfoCache[callingCode] ?: loadCallingCodeInfo(callingCode)?.also {
            callingCodeInfoCache[callingCode] = it
        }
    }

    override fun getCallingCodeForCountry(countryCode: String): String? {
        return countryCallingCode[countryCode.lowercase()]
    }

    override fun getCountriesForCallingCode(callingCode: String): List<String>? {
        return callingCodeCountries[callingCode]
    }

    override fun getDefaultCallingCode(): String? {
        return defaultCallingCode
    }

    override fun getAllCountryCallingCodes(): Map<String, String> {
        return countryCallingCode.toMap()
    }
}