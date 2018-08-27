package com.ft.ftchinese.util

import android.util.Log
import com.ft.ftchinese.models.ErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.Request
import java.io.IOException

val gson = Gson()

class Fetch {

    companion object {
        private const val TAG = "Fetch"

        private val client = OkHttpClient()

        private val jsonType = MediaType.parse("application/json")

        fun simpleGet(url: String): String? {

            return try {
                val request = Request.Builder()
                        .url(url)
                        .get()
                        .build()

                val response = client.newCall(request).execute()
                response.body()?.string()

            } catch (e: IllegalStateException) {
                // Request.build() error
                Log.w(TAG, "Empty url: $e")
                null
            } catch (e: IOException) {
                // Call.execute() error
                Log.w(TAG, "OkHttpClient execute/read body error: $e")
                null
            }
        }

        fun get(url: String, uuid: String? = null): Response {
            val builder = Request.Builder()
                    .url(url)
                    .get()
            if (uuid != null) {
                builder.header("X-User-Id", uuid)
            }

            return execute(builder)
        }

        fun post(url: String, content: String, uuid: String? = null): Response {
            val body = RequestBody.create(jsonType, content)
            val builder = Request.Builder()
                    .header("X-Client-Type", "android")
                    .header("X-Client-Version", "0.0.1")
                    .cacheControl(CacheControl.Builder().noCache().noStore().noTransform().build())
                    .url(url)
                    .post(body)

            if (uuid != null) {
                builder.header("X-User-Id", uuid)
            }

            return execute(builder)
        }

        /**
         * @return okhttp3.Response or null if there is any error thrown
         * @throws ErrorResponse If HTTP response status is above 400.
         * @throws IllegalStateException If request url is empty
         * @throws IOException If network request failed, or API returned error response but the response body could not be turned into string.
         * @throws JsonSyntaxException If API returned error response and the response body is decoced into a string, but the string could not be parsed into valid JSON.
         */
        private fun execute(builder: Request.Builder): Response {

            // If url is null, `build()` throws IllegalStateException
            // `execute()` throws IOException and IllegalStateException
            val response = client.newCall(builder.build()).execute()

            // If response is successful, return the response so that response body could be processed by the caller.
            if (response.isSuccessful) {
                return response
            }

            // If the response if not successful (status code >= 400), API returns body containing error details.
            // Wrap the response body and rethrow it.
            Log.w(TAG, "Request $builder failed")

            // `string()` throws IOException
            val body = response.body()?.string()

            // `fromJson()` throws JsonSyntaxException
            val errResp = gson.fromJson<ErrorResponse>(body, ErrorResponse::class.java)

            errResp.statusCode = response.code()

            throw errResp
        }
    }
}