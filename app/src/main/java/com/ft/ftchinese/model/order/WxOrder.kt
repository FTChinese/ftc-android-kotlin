package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.*
import org.threeten.bp.ZonedDateTime

data class WxOrder(
        override val id: String,
        @KTier
        override val tier: Tier,
        @KCycle
        override val cycle: Cycle,
        override val cycleCount: Long,
        override val extraDays: Long,
        override var amount: Double,
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
        amount = amount,
        usageType = usageType,
        payMethod = payMethod,
        createdAt = createdAt
)
