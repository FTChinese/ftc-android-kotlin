package com.ft.ftchinese.ui.checkout

data class PaymentButton (
    val enabled: Boolean,
    val text: String,
)

data class PaymentMethodsEnabled (
    val alipay: Boolean,
    val wechat: Boolean,
    val stripe: Boolean,
)
