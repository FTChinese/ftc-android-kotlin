package com.ft.ftchinese.models

import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.gson

data class SignUp(
        val email: String,
        val password: String
) {
    fun send(): Account {
        val response = Fetch().post((NextApi.SIGN_UP))
                .setClient()
                .noCache()
                .body(this@SignUp)
                .end()

        val body = response.body()?.string()

        return gson.fromJson<Account>(body, Account::class.java)
    }
}