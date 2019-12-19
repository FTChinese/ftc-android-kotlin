package com.ft.ftchinese.viewmodel

enum class WxRefreshState {
    SUCCESS, // Wx account is successfully refreshed.
    ReAuth // Wx account cannot be refreshed because the acces token stored server-side is expired or invalid.
}
