package com.ft.ftchinese.model.reader

import com.beust.klaxon.Json

open class BaseAccount(
    @Json(name = "id")
    open val id: String,
    @Json(name = "unionId")
    open val unionId: String? = null,
    @Json(name = "stripeId")
    open val stripeId: String? = null,
    @Json(name = "email")
    open val email: String,
    @Json(name = "mobile")
    open val mobile: String? = null,
    @Json(name = "userName")
    open val userName: String? = null,
    @Json(name = "avatarUrl")
    open val avatarUrl: String? = null,
    @Json(name = "isVerified")
    open val isVerified: Boolean = false,
    @Json(name = "campaignCode")
    open val campaignCode: String? = null,
)
