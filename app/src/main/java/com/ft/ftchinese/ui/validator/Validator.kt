package com.ft.ftchinese.ui.validator

import com.ft.ftchinese.R
import java.util.regex.Pattern

object Validator {
    fun ensureEmail(email: String): Int? {
        if (email.isBlank()) {
            return R.string.error_field_required
        }

        if (!email.contains("@")) {
            return R.string.error_invalid_email
        }

        return null
    }

    fun ensurePassword(pw: String): Int? {
        if (pw.isBlank()) {
            return R.string.error_field_required
        }

        if (pw.length < 8) {
            return R.string.error_invalid_password
        }

        return null
    }

    // Verification code for password reset
    fun validateCode(c: String): Boolean {
        return c.length == 6
    }

    fun ensureLength(s: String?, min: Int, max: Int): Int? {
        if (s.isNullOrBlank()) {
            return R.string.error_field_required
        }

        if (s.length < min ) {
            return R.string.error_too_short
        }

        if (s.length > max) {
            return R.string.error_too_long
        }

        return null
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
    fun isMainlandPhone(str: String): Boolean {
        val regExp = "^((13[0-9])|(15[^4])|(18[0,2,3,5-9])|(17[0-8])|(147))\\d{8}$"
        val p: Pattern = Pattern.compile(regExp)
        val m = p.matcher(str)
        return m.matches()
    }

    /**
     * 香港手机号码8位数，5|6|8|9开头+7位任意数
     */
    fun isHKPhone(str: String): Boolean {
        val regExp = "^(5|6|8|9)\\d{7}$"
        val p = Pattern.compile(regExp)
        val m = p.matcher(str)
        return m.matches()
    }
}
