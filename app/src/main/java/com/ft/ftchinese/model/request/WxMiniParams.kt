package com.ft.ftchinese.model.request

import kotlinx.serialization.Serializable

@Serializable
data class WxMiniParams(
    val id: String,
    val path: String,
)
