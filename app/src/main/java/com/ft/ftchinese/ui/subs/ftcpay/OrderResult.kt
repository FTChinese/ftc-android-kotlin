package com.ft.ftchinese.ui.subs.ftcpay

import com.ft.ftchinese.model.ftcsubs.AliPayIntent
import com.ft.ftchinese.model.ftcsubs.WxPayIntent

sealed class OrderResult {
    data class WxPay(val intent: WxPayIntent) : OrderResult()
    data class AliPay(val intent: AliPayIntent) : OrderResult()
}

