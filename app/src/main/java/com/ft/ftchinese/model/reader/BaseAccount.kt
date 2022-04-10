package com.ft.ftchinese.model.reader

import kotlinx.serialization.Serializable

@Serializable
data class BaseAccount(
    val id: String,
    val unionId: String? = null,
    val stripeId: String? = null,
    val email: String,
    val mobile: String? = null,
    val userName: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false,
    val campaignCode: String? = null,
)
