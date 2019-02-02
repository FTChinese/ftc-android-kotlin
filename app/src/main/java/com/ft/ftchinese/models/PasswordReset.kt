package com.ft.ftchinese.models

import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import java.io.IOException

data class PasswordReset(
        val email: String
) {
    /**
     * @return HTTP status code
     * @throws ClientError If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     */
    fun send(): Boolean {

        val (response, _)= Fetch()
                .post(NextApi.PASSWORD_RESET)
                .noCache()
                .jsonBody(json.toJsonString(this))
                .responseApi()

        return response.code() == 204
    }
}