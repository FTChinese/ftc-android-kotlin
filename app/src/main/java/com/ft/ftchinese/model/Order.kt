package com.ft.ftchinese.model

import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.OrderUsage
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.util.*
import org.threeten.bp.ZonedDateTime

//data class AlipayOrder(
//        val ftcOrderId: String,
//        val listPrice: Double,
//        val netPrice: Double,
//        val param: String
//)

data class AliOrder(
       override val id: String,
       @KTier
       override val tier: Tier,
       @KCycle
       override val cycle: Cycle,
       override val cycleCount: Long,
       override val extraDays: Long,
       override var netPrice: Double,
       @KOrderUsage
       override val usageType: OrderUsage,
       @KPayMethod
       override val payMethod: PayMethod,
       @KDateTime
       override val createdAt: ZonedDateTime,
       val param: String

) : Subscription(
        id = id,
        tier = tier,
        cycle = cycle,
        cycleCount = cycleCount,
        extraDays = extraDays,
        netPrice = netPrice,
        usageType = usageType,
        payMethod = payMethod,
        createdAt = createdAt
)

//data class WxPrepayOrder(
//        val ftcOrderId: String,
//        val listPrice: Double,
//        val netPrice: Double,
//        val appId: String,
//        val partnerId: String,
//        val prepayId: String,
//        val timestamp: String,
//        val nonce: String,
//        val pkg: String,
//        val signature: String
//)

data class WxOrder(
        override val id: String,
        @KTier
        override val tier: Tier,
        @KCycle
        override val cycle: Cycle,
        override val cycleCount: Long,
        override val extraDays: Long,
        override var netPrice: Double,
        @KOrderUsage
        override val usageType: OrderUsage,
        @KPayMethod
        override val payMethod: PayMethod,
        @KDateTime
        override val createdAt: ZonedDateTime,
        val appId: String,
        val partnerId: String,
        val prepayId: String,
        val timestamp: String,
        val nonce: String,
        val pkg: String,
        val signature: String
) : Subscription(
        id = id,
        tier = tier,
        cycle = cycle,
        cycleCount = cycleCount,
        extraDays = extraDays,
        netPrice = netPrice,
        usageType = usageType,
        payMethod = payMethod,
        createdAt = createdAt
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


