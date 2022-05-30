package com.ft.ftchinese.ui.account

import androidx.annotation.StringRes
import com.ft.ftchinese.R

enum class AccountAppScreen(@StringRes val titleId: Int) {
    Overview(
        titleId = R.string.title_account
    ),
    Email(
        titleId = R.string.title_change_email
    ),
    UserName(
        titleId = R.string.title_change_username
    ),
    Password(
        titleId = R.string.title_change_password
    ),
    Address(
        titleId = R.string.title_change_address
    ),
    Stripe(
        titleId = R.string.stripe_setting
    ),
    Wechat(
        titleId = R.string.title_wx_account
    ),
    Mobile(
        titleId = R.string.title_set_mobile
    ),
    DeleteAccount(
        titleId = R.string.title_delete_account
    ),
    // Verify an existing email + password. If valid, go to Merge screen.
    LinkCurrentEmail(titleId = R.string.title_link_email),
    // This is used when both accounts exists. Linking to newly created account does not need this.
    MergeWxEmail(titleId = R.string.title_merge_accounts),
    // Create a new email account and link to logged-in wechat.
    LinkNewEmail(titleId = R.string.title_sign_up),
    // If user forgot password when linking to current email.
    ForgotPassword(titleId = R.string.title_forgot_password),
    ResetPassword(titleId = R.string.title_reset_password),
    UnlinkWx(
        titleId = R.string.title_unlink
    );

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): AccountAppScreen =
            when (route?.substringBefore("/")) {
                Overview.name -> Overview
                Email.name -> Email
                UserName.name -> UserName
                Password.name -> Password
                Address.name -> Address
                Stripe.name -> Stripe
                Wechat.name -> Wechat
                Mobile.name -> Mobile
                DeleteAccount.name -> DeleteAccount
                LinkCurrentEmail.name -> LinkCurrentEmail
                MergeWxEmail.name -> MergeWxEmail
                LinkNewEmail.name -> LinkNewEmail
                ForgotPassword.name -> ForgotPassword
                ResetPassword.name -> ResetPassword
                UnlinkWx.name -> UnlinkWx
                null -> Overview
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
