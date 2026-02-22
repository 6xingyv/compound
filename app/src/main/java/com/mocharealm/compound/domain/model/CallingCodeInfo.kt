package com.mocharealm.compound.domain.model

data class CallingCodeInfo(
    val callingCode: String,
    val countries: List<String>,
    val trunkPrefixes: List<String> = emptyList(),
    val intlPrefixes: List<String> = emptyList(),
    val ruleSets: List<RuleSet> = emptyList()
) {
    fun format(number: String): String {
        val plainNumber = number.filter { it.isDigit() }

        for (ruleSet in ruleSets) {
            if (plainNumber.length < ruleSet.matchLen) continue

            for (rule in ruleSet.rules) {
                if (plainNumber.length > rule.maxLen) continue

                val num = plainNumber.toLongOrNull() ?: continue
                if (num in rule.minVal..rule.maxVal) {
                    return applyRule(rule, plainNumber)
                }
            }
        }

        return formatDefault(plainNumber)
    }

    fun formatDefault(number: String): String {
        return when {
            number.length <= 3 -> number
            number.length <= 7 -> "${number.substring(0, 3)} ${number.substring(3)}"
            number.length <= 10 -> "${number.substring(0, 3)} ${number.substring(3, 6)} ${number.substring(6)}"
            else -> {
                val parts = mutableListOf<String>()
                var start = 0
                while (start < number.length) {
                    val end = minOf(start + 4, number.length)
                    parts.add(number.substring(start, end))
                    start = end
                }
                parts.joinToString(" ")
            }
        }
    }
    fun applyRule(rule: PhoneRule, number: String): String {
        var result = rule.format
        val digits = number.toCharArray()
        var digitIndex = 0

        val sb = StringBuilder()
        var i = 0
        while (i < result.length) {
            when {
                result[i] == '#' -> {
                    if (digitIndex < digits.size) {
                        sb.append(digits[digitIndex])
                        digitIndex++
                    }
                    i++
                }
                result.startsWith("$", i) -> {
                    i++
                }
                else -> {
                    sb.append(result[i])
                    i++
                }
            }
        }

        return sb.toString()
    }

    fun isValid(number: String): Boolean {
        val plainNumber = number.filter { it.isDigit() }

        for (ruleSet in ruleSets) {
            if (plainNumber.length < ruleSet.matchLen) continue

            for (rule in ruleSet.rules) {
                if (plainNumber.length > rule.maxLen) continue

                val num = plainNumber.toLongOrNull() ?: continue
                if (num in rule.minVal..rule.maxVal) {
                    return true
                }
            }
        }

        return false
    }
}

data class RuleSet(
    val matchLen: Int,
    val rules: List<PhoneRule>,
    var hasRuleWithIntlPrefix: Boolean = false,
    var hasRuleWithTrunkPrefix: Boolean = false
)

data class PhoneRule(
    val minVal: Int,
    val maxVal: Int,
    val byte8: Int,
    val maxLen: Int,
    val otherFlag: Int,
    val prefixLen: Int,
    val flag12: Int,
    val flag13: Int,
    val format: String,
) {
    val hasIntlPrefix: Boolean get() = (otherFlag and 0x01) != 0
}