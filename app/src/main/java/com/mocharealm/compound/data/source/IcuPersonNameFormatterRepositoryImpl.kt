package com.mocharealm.compound.data.source

import android.annotation.SuppressLint
import com.mocharealm.compound.domain.model.User
import com.mocharealm.compound.domain.repository.NameLength
import com.mocharealm.compound.domain.repository.PersonNameFormatterRepository
import java.lang.reflect.Proxy
import java.util.Locale

@SuppressLint("PrivateApi")
class IcuPersonNameFormatterRepositoryImpl: PersonNameFormatterRepository {

    private val formatterObj by lazy { createFormatter() }
    private val formatMethod by lazy {
        Class.forName("android.icu.text.PersonNameFormatter")
            .getMethod("formatToString", Class.forName("android.icu.text.PersonName"))
    }

    override fun formatName(user: User, length: NameLength): String {
        return try {
            val personNameProxy = createPersonNameProxy(user)
            formatMethod.invoke(formatterObj, personNameProxy) as String
        } catch (e: Exception) {
            // 回退方案：如果反射失败或 API 不支持，手动拼接
            "${user.firstName} ${user.lastName}".trim()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createPersonNameProxy(user: User): Any {
        val personNameInterface = Class.forName("android.icu.text.PersonName")
        val nameFieldClass = Class.forName("android.icu.text.PersonName\$NameField")

        val fields = nameFieldClass.enumConstants?.associateBy { it.toString() } ?: emptyMap()

        val fieldGiven = fields["GIVEN"]
        val fieldSurname = fields["SURNAME"]

        return Proxy.newProxyInstance(
            personNameInterface.classLoader,
            arrayOf(personNameInterface)
        ) { _, method, args ->
            when (method.name) {
                "getFieldValue" -> {
                    val requestedField = args[0] // 这是一个枚举对象

                    // 使用 equals 比较枚举实例，比 toString 包含判断更安全
                    when (requestedField) {
                        fieldGiven -> user.firstName
                        fieldSurname -> user.lastName
                        else -> null
                    }
                }
                "getLocale" -> Locale.getDefault()
                "getDisplayOrder" -> null // 返回 null 表示由系统决定
                else -> null
            }
        }
    }

    private fun createFormatter(): Any {
        val cls = Class.forName("android.icu.text.PersonNameFormatter")
        val builder = cls.getMethod("builder").invoke(null)
        return builder.javaClass.getMethod("build").invoke(builder)
    }
}