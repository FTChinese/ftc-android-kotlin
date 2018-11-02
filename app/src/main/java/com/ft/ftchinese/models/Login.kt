package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.IOException

data class Login(
        val email: String,
        val password: String
) : AnkoLogger {
    /**
     * @return Account
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    fun send(): Account {
        val response = Fetch().post(NextApi.LOGIN)
                .setClient()
                .noCache()
                .body(this@Login)
                .end()

        val body = response.body()?.string()
        info("Response body: $body")

        return gson.fromJson<Account>(body, Account::class.java)
    }
}