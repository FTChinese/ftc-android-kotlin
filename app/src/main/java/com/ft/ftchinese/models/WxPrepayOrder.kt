package com.ft.ftchinese.models


data class WxPrepayOrder(
        val ftcOrderId: String,
        val appid: String,
        val partnerid: String,
        val prepayid: String,
        val noncestr: String,
        val timestamp: String,
        val `package`: String,
        val sign: String
)