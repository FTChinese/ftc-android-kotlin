package com.ft.ftchinese.models

import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import java.io.IOException

data class EmailUpdate(
        val email: String
) {
    /**
     * @return nothing if proceeded successfully
     * @throws ErrorResponse If HTTP response status is above 400.
     * @throws IllegalStateException If request url is empty.
     * @throws IOException If network request failed, or response body can not be read, regardless of if response is successful or not.
     * @throws JsonSyntaxException If the content returned by API could not be parsed into valid JSON, regardless of if response is successful or not
     */
    suspend fun send(uuid: String) {
        val job = async {
            Fetch.post(ApiEndpoint.UPDATE_EMAIL, gson.toJson(this@EmailUpdate), uuid)
        }

        val response = job.await()

        Log.i("EmailUpdate", "Response status code: ${response.code()}")
    }
}

data class UserNameUpdate(
        val name: String
) {

}

data class PasswordUpdate(
        val oldPassword: String,
        val newPassword: String
) {

}