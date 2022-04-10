package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.enums.UnlinkAnchor
import kotlinx.serialization.Serializable

@Serializable
data class WxUnlinkParams(
    val ftcId: String,
    val anchor: UnlinkAnchor? = null,
)
