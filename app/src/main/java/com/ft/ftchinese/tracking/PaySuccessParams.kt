package com.ft.ftchinese.tracking

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.ftcsubs.FtcPayIntent
import com.ft.ftchinese.model.paywall.CartItemStripe

data class PaySuccessParams(
    val edition: Edition,
    val currency: String,
    val amountPaid: Double,
    val payMethod: PayMethod,
) {
    companion object {
        @JvmStatic
        fun ofFtc(pi: FtcPayIntent): PaySuccessParams {
            return PaySuccessParams(
                edition = pi.price.edition,
                currency = pi.price.currency,
                amountPaid = pi.order.payableAmount,
                payMethod = pi.order.payMethod,
            )
        }

        @JvmStatic
        fun ofStripe(item: CartItemStripe): PaySuccessParams {
            return PaySuccessParams(
                edition = item.recurring.edition,
                currency = item.recurring.currency,
                amountPaid = if (item.trial != null) {
                    item.trial.moneyAmount
                } else {
                    item.recurring.moneyAmount
                },
                payMethod = PayMethod.STRIPE,
            )
        }
    }
}
