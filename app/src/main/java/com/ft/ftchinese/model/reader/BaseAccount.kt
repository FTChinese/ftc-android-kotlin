package com.ft.ftchinese.model.reader

open class BaseAccount(
    open val id: String,
    open val unionId: String? = null,
    open val stripeId: String? = null,
    open val email: String,
    open val mobile: String? = null,
    open val userName: String? = null,
    open val avatarUrl: String? = null,
    open val isVerified: Boolean = false,
    open val campaignCode: String? = null,
)
