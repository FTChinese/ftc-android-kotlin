package com.ft.ftchinese.models

import android.util.Log
import com.ft.ftchinese.util.ApiEndpoint
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.experimental.async
import java.io.IOException

data class Account(
        val email: String,
        val password: String
) {
    suspend fun send (url: String): User? {
        val job = async {
            Fetch.post(url, gson.toJson(this@Account))
        }
        val response = job.await()

        Log.i(TAG,"Response code: ${response.code()}")
        Log.i(TAG,"Response message: ${response.message()}")

        Log.i(TAG,"Is successful: ${response.isSuccessful}")

        val body: String?
        try {
            body = response.body()?.string()
        } catch (e: IOException) {
            Log.i(TAG, "Cannot get response body")
            return null
        }
        if (!response.isSuccessful) {
            try {
                val err = gson.fromJson<ErrorResponse>(body, ErrorResponse::class.java)
                throw err
            } catch (e: JsonSyntaxException) {
                Log.i(TAG, "Parse ErrorResponse error")
                return null
            }
        }

        return try {
            gson.fromJson<User>(body, User::class.java)
        } catch (e: JsonSyntaxException) {
            Log.i(TAG, "Parse JSON error")
            null
        }
    }

    suspend fun login(): User? {
        return send(ApiEndpoint.LOGIN)
    }

    suspend fun create(): User? {
        return send(ApiEndpoint.NEW_ACCOUNT)
    }

    companion object {
        private const val TAG = "Account"
    }
}