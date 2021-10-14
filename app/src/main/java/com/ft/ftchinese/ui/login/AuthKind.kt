package com.ft.ftchinese.ui.login

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// AuthKind is used to determine how the LogInFragment and SignInFragment will be used.
@Parcelize
enum class AuthKind : Parcelable {
    EmailLogin, // User email + password to login
    MobileLink, // Mobile login for the first time and link to an existing email account
    WechatLink; // Wechat-logged-in user is trying to link to an existing email account.
}
