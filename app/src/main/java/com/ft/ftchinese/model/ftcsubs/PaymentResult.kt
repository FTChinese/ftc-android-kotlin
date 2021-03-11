package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KPayMethod
import org.threeten.bp.ZonedDateTime

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
    @KDateTime
    val paidAt: ZonedDateTime? = null, // ISO8601
    @KPayMethod
    val payMethod: PayMethod? = null
) {
    fun isVerified(): Boolean {
        return arrayOf("TRADE_SUCCESS", "SUCCESS").contains(paymentState)
    }
}

