package com.ft.ftchinese.model.subscription

import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier

data class Edition(
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle
)
