package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.util.KPayMethod

val paymentSuccessStates = arrayOf("TRADE_SUCCESS", "SUCCESS")

/**
 * After WXPayEntryActivity received result 0, check against our server for the payment result.
 * Server will in turn check payment result from Wechat server.
 */
data class PaymentResult(
    val paymentState: String,
    val paymentStateDesc: String,
    val totalFee: Int,
    val transactionId: String,
    val ftcOrderId: String,
    val paidAt: String? = null, // ISO8601
    @KPayMethod
    val payMethod: PayMethod? = null
) {
    fun isOrderPaid(): Boolean {
        return paymentSuccessStates.contains(paymentState)
    }
}

data class VerificationResult(
    val order: Order,
    val payment: PaymentResult,
    val membership: Membership
)
