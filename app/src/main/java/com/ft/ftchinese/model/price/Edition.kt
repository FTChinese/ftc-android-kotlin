package com.ft.ftchinese.model.price

import com.ft.ftchinese.model.enums.Cycle
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier

data class Edition(
    @KTier
    val tier: Tier,
    @KCycle
    val cycle: Cycle
)
