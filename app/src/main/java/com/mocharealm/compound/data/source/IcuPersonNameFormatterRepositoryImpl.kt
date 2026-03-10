package com.mocharealm.compound.data.source

import android.annotation.SuppressLint
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.repository.NameLength
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Proxy
import java.util.Locale

@SuppressLint("PrivateApi")
class IcuPersonNameFormatterRepositoryImpl : PersonNameFormatterRepository {
    init {
        HiddenApiBypass.addHiddenApiExemptions("Landroid/icu/text/")
    }

    private fun createFormatter(): Any? {
        val cls = Class.forName("android.icu.text.PersonNameFormatter")
        // 使用 HiddenApiBypass 获取 builder 方法
        val builderMethod = HiddenApiBypass.getDeclaredMethod(cls, "builder")
        val builder = builderMethod.invoke(null)

        val buildMethod = HiddenApiBypass.getDeclaredMethod(builder.javaClass, "build")
        return buildMethod.invoke(builder)
    }
    private val formatterObj by lazy { createFormatter() }
    private val formatMethod by lazy {
        val personNameClass = Class.forName("android.icu.text.PersonName")
        HiddenApiBypass.getDeclaredMethod(
            Class.forName("android.icu.text.PersonNameFormatter"),
            "formatToString",
            personNameClass
        )
    }

    private fun detectLocale(user: User): Locale {
        val fullText = user.firstName + user.lastName
        val hasChinese = fullText.any { char ->
            Character.UnicodeBlock.of(char) in listOf(
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            )
        }

        return if (hasChinese) {
            Locale.CHINESE
        } else {
            Locale.ENGLISH
        }
    }

    @SuppressLint("PrivateApi")
    override fun formatName(user: User, length: NameLength): String {
        return try {
            val personNameProxy = createPersonNameProxy(user)
            formatMethod.invoke(formatterObj, personNameProxy) as String
        } catch (e: Exception) {
            android.util.Log.e("ICU_Formatter", "Format failed", e)
            "${user.lastName}${user.firstName}".trim() // 降级方案
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createPersonNameProxy(user: User): Any {
        val personNameInterface = Class.forName("android.icu.text.PersonName")
        val preferredOrderClass = Class.forName("android.icu.text.PersonName\$PreferredOrder")

        val detectedLocale = detectLocale(user)

        return Proxy.newProxyInstance(
            personNameInterface.classLoader,
            arrayOf(personNameInterface)
        ) { proxy, method, args ->
            when (method.name) {
                "getFieldValue" -> {
                    val field = args[0].toString()
                    when (field) {
                        "given" -> user.firstName
                        "surname" -> user.lastName
                        else -> null
                    }
                }
                "getNameLocale" -> detectedLocale
                "getPreferredOrder" -> {
                    // 返回 PreferredOrder.DEFAULT (由系统根据 Locale 自动推断)
                    java.lang.Enum.valueOf(preferredOrderClass as Class<out Enum<*>>, "DEFAULT")
                }
                "equals" -> proxy === args[0]
                "hashCode" -> user.hashCode()
                "toString" -> "PersonNameProxy(${user.firstName} ${user.lastName})"
                else -> null
            }
        }
    }
}