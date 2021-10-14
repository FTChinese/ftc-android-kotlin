package com.ft.ftchinese.model.request

import com.ft.ftchinese.model.fetch.json

data class Credentials(
    val email: String,
    val password: String,
    val mobile: String? = null, // Required when login with mobile and user chooses to create a new email account.
    val deviceToken: String,
) {
    fun toJsonString(): String {
        return json.toJsonString(this)
    }
}


