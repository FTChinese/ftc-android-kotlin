package com.ft.ftchinese.model.enums

import kotlinx.serialization.Serializable

@Serializable
open class Edition(
    open val tier: Tier,
    open val cycle: Cycle
)
