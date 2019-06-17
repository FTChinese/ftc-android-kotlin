package com.ft.ftchinese.wxapi

data class UIWxOAuth (
        val heading: Int,
        val body: String? = null,
        val done: Boolean = false,
        val restartLogin: Boolean = false
)
