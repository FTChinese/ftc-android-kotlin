package com.ft.ftchinese.ui.account.stripewallet

import com.ft.ftchinese.model.stripesubs.StripePaymentMethod

data class PaymentMethodInUse(
    val current: StripePaymentMethod?,
    val isDefault: Boolean,
)

