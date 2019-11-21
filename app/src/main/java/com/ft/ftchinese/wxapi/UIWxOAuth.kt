package com.ft.ftchinese.wxapi

data class UIWxOAuth (
        val heading: String,
        var body: String? = null,
        val done: Boolean = false,
        val restartLogin: Boolean = false
)
