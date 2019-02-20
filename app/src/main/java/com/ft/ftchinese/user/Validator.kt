package com.ft.ftchinese.user

import com.ft.ftchinese.R

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
}