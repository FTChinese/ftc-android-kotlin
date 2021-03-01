package com.ft.ftchinese.model.addon

import com.ft.ftchinese.model.enums.CarryOverSource
import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCarryOverSource
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KTier
import org.threeten.bp.ZonedDateTime

data class AddOn(
    val id: String,
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle,
    val cycleCount: Int,
    val daysRemained: Int,
    @KCarryOverSource
    val carryOverSource: CarryOverSource,
    val payMethod: PayMethod,
    val compoundId: String,
    val orderId: String?,
    val priceId: String?,
    @KDateTime
    val createdUtc: ZonedDateTime?,
    @KDateTime
    val consumedUtc: ZonedDateTime?,
)
