package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import org.jetbrains.anko.AnkoLogger
import java.io.IOException

data class Login(
        val email: String,
        val password: String
) : AnkoLogger {
    /**
     * @return Account
     * @throws ClientError If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     */
    fun send(): Account? {
        val (_, body) = Fetch().post(NextApi.LOGIN)
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