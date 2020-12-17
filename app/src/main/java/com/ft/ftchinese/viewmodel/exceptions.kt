package com.ft.ftchinese.viewmodel

import com.ft.ftchinese.model.subscription.Checkout
import com.ft.ftchinese.model.subscription.PaymentIntent
import java.lang.Exception

data class IdempotencyError(
        override val message: String? = "problem with idempotency key"
) : Exception(message)

data class FreeUpgradeDeniedError(
        val checkout: Checkout
) : Exception("free upgrade is not allowed")
