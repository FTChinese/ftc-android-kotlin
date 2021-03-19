package com.ft.ftchinese.ui.validator

import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Address

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

    fun ensureUserName(name: String): Int? {
        if (name.isBlank()) {
            return R.string.error_field_required
        }

        if (name.length > 256) {
            return R.string.error_too_long
        }

        return null
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
}
