package com.ft.ftchinese.model.paywall

data class Banner(
    val id: String,
    val heading: String,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null
)
