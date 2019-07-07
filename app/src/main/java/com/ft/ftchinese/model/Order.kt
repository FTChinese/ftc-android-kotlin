package com.ft.ftchinese.model

data class AlipayOrder(
        val ftcOrderId: String,
        val listPrice: Double,
        val netPrice: Double,
        val param: String
)

data class WxPrepayOrder(
        val ftcOrderId: String,
        val listPrice: Double,
        val netPrice: Double,
        val appId: String,
        val partnerId: String,
        val prepayId: String,
        val timestamp: String,
        val nonce: String,
        val pkg: String,
        val signature: String
)

/**
 * After WXPayEntryActivity received result 0, check against our server for the payment result.
 * Server will in turn check payment result from Wechat server.
 */
data class WxOrderQuery(
        val paymentState: String,
        val paymentStateDesc: String,
        val totalFee: Int,
        val transactionId: String,
        val ftcOrderId: String,
        val paidAt: String // ISO8601
)


