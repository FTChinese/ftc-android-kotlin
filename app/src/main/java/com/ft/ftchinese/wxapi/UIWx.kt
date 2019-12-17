package com.ft.ftchinese.wxapi

data class UIWx (
        val heading: String,
        var body: String = "",
        val enableButton: Boolean = false
)
