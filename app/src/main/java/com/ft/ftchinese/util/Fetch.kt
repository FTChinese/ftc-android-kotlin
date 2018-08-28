package com.ft.ftchinese.util

import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.models.ErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import java.io.IOException

val gson = Gson()

class Fetch {

    private var method: String = "GET"
    private val headers = Headers.Builder()
    private val reqBuilder = Request.Builder()
    private var reqBody: RequestBody? = null

    private val contentType: MediaType? = MediaType.parse("application/json")
    private var cacheControl: CacheControl? = null

    fun get(url: String): Fetch {
        reqBuilder.url(url)
        return this
    }

    fun post(url: String): Fetch {
        reqBuilder.url(url)
        method = "POST"
        return this
    }

    fun patch(url: String): Fetch {
        reqBuilder.url(url)
        method = "PATCH"
        return this
    }

    fun delete(url: String): Fetch {
        reqBuilder.url(url)
        method = "DELETE"
        return this
    }

    fun header(name: String, value: String): Fetch {
        headers.set(name, value)
        return this
    }

    fun setClient(): Fetch {
        headers.set("X-Client-Type", "android")
                .set("X-Client-Version", BuildConfig.VERSION_NAME)
        return this
    }

    fun setUserId(uuid: String): Fetch {
        headers.set("X-User-Id", uuid)

        return this
    }

    fun noCache(): Fetch {
        cacheControl = CacheControl.Builder()
                .noCache()
                .noStore()
                .noTransform()
                .build()

        return this
    }

    fun body(o: Any): Fetch {
        reqBody = RequestBody.create(contentType, gson.toJson(o))
        return  this
    }

    fun end(): Response {
        reqBuilder.headers(headers.build())

        if (cacheControl != null) {
            reqBuilder.cacheControl(cacheControl)
        }

        reqBuilder.method(method, reqBody)

        return execute(reqBuilder)
    }

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
            Log.i(TAG, "Request to $url")
            val builder = Request.Builder()
                    .url(url)
                    .get()
            if (uuid != null) {
                builder.header("X-User-Id", uuid)
            }

            return execute(builder)
        }

        /**
         * Request body should be nullable since some API endpoint do not accept data when using post.
         */
        fun post(url: String, content: String? = null, uuid: String? = null): Response {
            Log.i(TAG, "Request to $url with body $content")

            val builder = Request.Builder()
                    .header("X-Client-Type", "android")
                    .header("X-Client-Version", "0.0.1")
                    .cacheControl(CacheControl.Builder().noCache().noStore().noTransform().build())
                    .url(url)

            if (content != null) {
                val body = RequestBody.create(jsonType, content)
                builder.post(body)
            }

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
            val request = builder.build()

            Log.i(TAG, "URL: ${request.url()}. Method: ${request.method()}. Headers: ${request.headers()}")

            val response = client.newCall(builder.build()).execute()


            Log.i(TAG, "Reponse code: ${response.code()}. Message: ${response.message()}")

            // If response is successful, return the response so that response body could be processed by the caller.
            if (response.isSuccessful) {
                return response
            }

            // If the response if not successful (status code >= 400), API returns body containing error details.
            // Wrap the response body and rethrow it.

            // `string()` throws IOException
            val body = response.body()?.string()

            // `fromJson()` throws JsonSyntaxException
            val errResp = gson.fromJson<ErrorResponse>(body, ErrorResponse::class.java)

            errResp.statusCode = response.code()

            throw errResp
        }
    }
}