package com.ft.ftchinese.models

import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

data class SignUp(
        val email: String,
        val password: String
) {
    suspend fun send(): Account {
        val response = GlobalScope.async {
            Fetch().post((NextApi.SIGN_UP))
                    .setClient()
                    .noCache()
                    .body(this@SignUp)
                    .end()
        }.await()

        val body = response.body()?.string()

        return gson.fromJson<Account>(body, Account::class.java)
    }
}