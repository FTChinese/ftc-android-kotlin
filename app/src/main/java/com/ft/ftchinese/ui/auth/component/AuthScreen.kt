package com.ft.ftchinese.ui.auth.component

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class AuthScreen(@StringRes val titleId: Int) {
    MobileLogin(titleId = R.string.title_login),
    MobileLinkEmail(titleId = R.string.title_link_email),
    EmailExists(titleId = R.string.label_email),
    EmailLogin(titleId = R.string.title_email_login),
    EmailSignUp(titleId = R.string.title_sign_up),
    ForgotPassword(titleId = R.string.title_forgot_password),
    ResetPassword(titleId = R.string.title_reset_password);

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): AuthScreen =
            when (route?.substringBefore("/")) {
                MobileLogin.name -> MobileLogin
                MobileLinkEmail.name -> MobileLinkEmail
                EmailExists.name -> EmailExists
                EmailLogin.name -> EmailLogin
                EmailSignUp.name -> EmailSignUp
                ForgotPassword.name -> ForgotPassword
                ResetPassword.name -> ResetPassword
                null -> MobileLogin
                else -> throw IllegalArgumentException("Route $route is not recognized")
        }
    }
}
