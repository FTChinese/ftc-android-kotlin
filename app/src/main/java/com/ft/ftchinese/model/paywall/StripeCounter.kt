package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.stripesubs.StripePrice

data class StripeCounter(
    val orderKind: OrderKind,
    val recurringPrice: StripePrice,
    val trialPrice: StripePrice?,
)
