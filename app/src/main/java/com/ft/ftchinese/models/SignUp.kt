package com.ft.ftchinese.models

import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.json

data class SignUp(
        val email: String,
        val password: String
) {
    fun send(): Account? {
        val (_, body) = Fetch()
                .post((NextApi.SIGN_UP))
                .setClient()
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return if (body == null) {
            null
        } else {
            json.parse<Account>(body)
        }
    }
}