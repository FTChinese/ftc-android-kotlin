package com.ft.ftchinese.model.subscription

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
        override var amount: Double,
        @KOrderUsage
        override val usageType: OrderUsage,
        @KPayMethod
        override val payMethod: PayMethod,
        @KDateTime
        override val createdAt: ZonedDateTime,
        val param: String

) : Order(
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
