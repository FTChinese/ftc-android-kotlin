package com.ft.ftchinese.ui.wxlink

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class LinkAppScreen(@StringRes val titleId: Int) {
    // Verify an existing email + password.
    // If valid, go to MergeWxEmail screen.
    CurrentEmail(titleId = R.string.title_link_email),
    MergeWxEmail(titleId = R.string.title_merge_accounts),
    // Create a new email account and link to logged-in wechat.
    // The new account is automatically linked to current wechat account.
    NewEmail(titleId = R.string.title_sign_up),
    ForgotPassword(titleId = R.string.title_forgot_password),
    ResetPassword(titleId = R.string.title_reset_password);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): LinkAppScreen =
            when (route?.substringBefore("/")) {
                CurrentEmail.name -> CurrentEmail
                MergeWxEmail.name -> MergeWxEmail
                NewEmail.name -> NewEmail
                ForgotPassword.name -> ForgotPassword
                ResetPassword.name -> ResetPassword
                null -> CurrentEmail
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
