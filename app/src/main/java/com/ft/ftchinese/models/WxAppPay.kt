package com.ft.ftchinese.models


data class WxAppPay(
        val appid: String,
        val timeStamp: String,
        val nonceStr: String,
        val `package`: String,
        val signType: String,
        val paySign: String
)