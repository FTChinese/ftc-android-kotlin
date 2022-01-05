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
     * 大陆手机号码11位数字，未来可能会增加
     */
    fun isMainlandPhone(str: String?): Boolean {
        if (str == null) {
            return false
        }

        if (BuildConfig.DEBUG) {
            return true
        }

        val regExp = "^\\d{11}$"
        val p: Pattern = Pattern.compile(regExp)
        return p.matcher(str).matches()
    }
}
