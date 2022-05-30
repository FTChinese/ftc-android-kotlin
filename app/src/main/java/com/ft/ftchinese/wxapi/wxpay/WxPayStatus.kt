package com.ft.ftchinese.wxapi.wxpay

import com.ft.ftchinese.model.reader.Membership

sealed class WxPayStatus {
    object Loading: WxPayStatus()
    data class Success(val membership: Membership): WxPayStatus()
    data class Error(val message: String): WxPayStatus() // Pitfall: Plain Java might pass a nullable value which cannot trigger data changes.
    object Canceled: WxPayStatus()
}
