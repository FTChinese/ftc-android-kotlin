package com.ft.ftchinese.models


data class WxPrepayOrder(
        val appid: String,
        val partnerid: String,
        val prepayid: String,
        val noncstr: String,
        val timestamp: String,
        val `package`: String,
        val sign: String
)