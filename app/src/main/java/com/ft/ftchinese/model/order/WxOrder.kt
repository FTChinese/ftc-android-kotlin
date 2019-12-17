package com.ft.ftchinese.model.order

import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.util.*
import org.threeten.bp.ZonedDateTime

data class WxPayParams(
        val appId: String,
        val partnerId: String,
        val prepayId: String,
        val timestamp: String,
        val nonce: String,
        val pkg: String,
        val signature: String
)

data class WxOrder(
        override val id: String,
        @KTier
        override val tier: Tier,
        @KCycle
        override val cycle: Cycle,
        override var amount: Double,
        override val cycleCount: Long,
        override val extraDays: Long,
        @KOrderUsage
        override val usageType: OrderUsage,
        @KPayMethod
        override val payMethod: PayMethod,
        @KDateTime
        override val createdAt: ZonedDateTime,
        val appId: String, // Deprecate
        val partnerId: String, // Deprecate
        val prepayId: String, // Deprecate
        val timestamp: String, // Deprecate
        val nonce: String, // Deprecate
        val pkg: String, // Deprecate
        val signature: String, // Deprecate
        val params: WxPayParams
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
