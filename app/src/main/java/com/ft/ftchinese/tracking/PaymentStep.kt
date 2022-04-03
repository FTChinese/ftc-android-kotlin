package com.ft.ftchinese.tracking

sealed class PaymentStep {
    object DisplayPaywall : PaymentStep()
    data class AddCart(val params: AddCartParams) : PaymentStep()
}
