package com.ft.ftchinese.model.ftcsubs

import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

/**
 * After WXPayEntryActivity received result 0, check against our server for the payment result.
 * Server will in turn check payment result from Wechat server.
 */
@Serializable
data class PaymentResult(
    val paymentState: String,
    val paymentStateDesc: String,
    val totalFee: Int,
    val transactionId: String,
    val ftcOrderId: String,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val paidAt: ZonedDateTime? = null, // ISO8601
    val payMethod: PayMethod? = null
) {
    fun isVerified(): Boolean {
        return arrayOf("TRADE_SUCCESS", "SUCCESS").contains(paymentState)
    }
}

