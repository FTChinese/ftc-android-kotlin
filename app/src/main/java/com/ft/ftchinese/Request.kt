package com.ft.ftchinese

import android.util.Log
import okhttp3.*
import okhttp3.Request

class Request {

    companion object {

        private val client = OkHttpClient()

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
            val body = RequestBody.create(MediaType.parse("application/json"), content)
            val request = Request.Builder()
                    .url(url)
                    .header("X-Client-Type", "android")
                    .header("X-Client-Version", "0.0.1")
                    .cacheControl(CacheControl.Builder().noCache().noStore().noTransform().build())
                    .post(body)
                    .build()
            return client.newCall(request).execute()
        }
    }
}