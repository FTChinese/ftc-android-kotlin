package com.ft.ftchinese.model.enums

import com.ft.ftchinese.model.fetch.KCycle
import com.ft.ftchinese.model.fetch.KTier

open class Edition(
    @KTier
    open val tier: Tier,
    @KCycle
    open val cycle: Cycle
) {
    val namedKey: String
        get() = "${tier}_${cycle}"
}
