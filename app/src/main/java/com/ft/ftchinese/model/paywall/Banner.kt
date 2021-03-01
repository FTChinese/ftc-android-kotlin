package com.ft.ftchinese.model.paywall

data class Banner(
    val id: Int,
    val heading: String,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null
)
