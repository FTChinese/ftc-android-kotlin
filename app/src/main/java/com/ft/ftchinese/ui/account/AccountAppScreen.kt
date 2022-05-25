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
    );

    companion object {
        @JvmStatic
        fun fromRoute(route: String?): AccountAppScreen =
            when (route?.substringBefore("/")) {
                Overview.name -> Overview
                Email.name -> Email
                UserName.name -> UserName
                Password.name -> Password
                null -> Overview
                else -> throw IllegalArgumentException("Route $route is not recognized")
            }
    }
}
