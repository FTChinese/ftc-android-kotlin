package com.ft.ftchinese.utils

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okhttp3.Request

val gson = Gson()

class Fetch {

    companion object {

        private val client = OkHttpClient()

        val jsonType = MediaType.parse("application/json")

        val baseRequest = Request.Builder()
                .header("X-Client-Type", "android")
                .header("X-Client-Version", "0.0.1")
                .cacheControl(CacheControl.Builder().noCache().noStore().noTransform().build())
                .build()

        fun get(url: String): String? {
            try {
                val request = Request.Builder()
                        .url(url)
                        .build()
                val response = client.newCall(request).execute()
                return response.body()?.string()
            } catch (e: Exception) {
                Log.w("requestData", e.toString())
            }

            return null
        }

        fun post(url: String, content: String): Response {
            val body = RequestBody.create(jsonType, content)
            val request = baseRequest
                    .newBuilder()
                    .url(url)
                    .post(body)
                    .build()

            return client.newCall(request).execute()
        }
    }
}