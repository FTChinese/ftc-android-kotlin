package com.ft.ftchinese.wxapi

import com.ft.ftchinese.model.reader.Membership

sealed class WxPayStatus {
    object Loading: WxPayStatus()
    data class Success(val membership: Membership): WxPayStatus()
    data class Error(val message: String): WxPayStatus()
    object Canceled: WxPayStatus()
}
