package com.ft.ftchinese.ui.auth

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class PasswordAppScreen(
    @StringRes val titleId: Int
) {
    Forgot(titleId = R.string.title_forgot_password),
    Reset(titleId = R.string.title_reset_password);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): PasswordAppScreen =
            when (route?.substringBefore("/")) {
                Forgot.name -> Forgot
                Reset.name -> Reset
                null -> Forgot
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
