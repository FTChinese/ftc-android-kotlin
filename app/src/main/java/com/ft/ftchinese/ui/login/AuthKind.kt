package com.ft.ftchinese.ui.login

// AuthKind is used to determine how the LogInFragment and SignInFragment will be used.
enum class AuthKind {
    EmailLogin, // User email + password to login
    MobileLink, // Mobile login for the first time and link to an existing email account
    WechatLink; // Wechat-logged-in user is trying to link to an existing email account.
}
