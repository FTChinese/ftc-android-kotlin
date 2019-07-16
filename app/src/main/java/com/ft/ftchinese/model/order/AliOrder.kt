package com.ft.ftchinese.model.order

import com.ft.ftchinese.util.*
import org.threeten.bp.ZonedDateTime

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
        override val paymentMethod: PayMethod,
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
        paymentMethod = paymentMethod,
        createdAt = createdAt
)
