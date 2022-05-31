package com.ft.ftchinese.wxapi.wxpay

sealed class WxPayStatus {
    object Loading: WxPayStatus()
    object Success: WxPayStatus()
    data class Error(val message: String): WxPayStatus() // Pitfall: Plain Java might pass a nullable value which cannot trigger data changes.
    object Canceled: WxPayStatus()
}
