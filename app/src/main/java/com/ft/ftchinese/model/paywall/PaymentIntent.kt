package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod

/**
 * Collects all data required for final payment after
 * user selected a payment method.
 */
data class PaymentIntent (
    val item: FtcCheckout,
    val orderKind: OrderKind,
    val payMethod: PayMethod
)
