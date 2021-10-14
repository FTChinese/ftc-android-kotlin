package com.ft.ftchinese.ui.validator

import android.util.Patterns
import com.ft.ftchinese.BuildConfig
import java.util.regex.Pattern

object Validator {

    fun notEmpty(s: String?): Boolean {
        return !s?.trim().isNullOrBlank()
    }

    fun containNoSpace(s: String?): Boolean {
        if (s == null) {
            return true
        }
        return s.indexOf(" ") < 0
    }

    fun minLength(l: Int): Predicate {
        return fun (s: String?): Boolean {
            return s?.trim()?.length?.let {
                it >= l
            } ?: false
        }
    }

    fun maxLength(l: Int): Predicate {
        return fun(s: String?): Boolean {
            return s?.trim()?.length?.let {
                it <= l
            } ?: false
        }
    }

    fun isEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) {
            return false
        }

        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * 大陆手机号码11位数，匹配格式：前三位固定格式+后8位任意数
     * 此方法中前三位格式有：
     * 13+任意数
     * 15+除4的任意数
     * 18+除1和4的任意数
     * 17+除9的任意数
     * 147
     */
    fun isMainlandPhone(str: String?): Boolean {
        if (str == null) {
            return false
        }

        if (BuildConfig.DEBUG) {
            return true
        }

        val regExp = "^((13[0-9])|(15[^4])|(18[0,2,3,5-9])|(17[0-8])|(147))\\d{8}$"
        val p: Pattern = Pattern.compile(regExp)
        return p.matcher(str).matches()
    }
}
