package com.ft.ftchinese.model.reader

import kotlinx.serialization.Serializable

@Serializable
data class MobileEmailLinked(
    val id: String?,
    @Transient
    val mobile: String = ""
)
