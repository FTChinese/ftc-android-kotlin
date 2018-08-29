package com.ft.ftchinese.models

import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import java.io.IOException

data class PasswordReset(
        val email: String
) {
    /**
     * @return nothing if proceeded successfully
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    suspend fun send() {
        val job = async {

            Fetch().post(ApiEndpoint.PASSWORD_RESET)
                    .noCache()
                    .body(this@PasswordReset)
                    .end()
        }

        val response = job.await()

        val body = response.body()?.string()

        Log.i("PasswordReset", body)
    }
}