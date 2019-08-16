package com.ft.ftchinese.util

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import okhttp3.*
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.IOException
import java.util.concurrent.TimeUnit

val statusCodeMeaning = mapOf(
        401 to R.string.api_unauthorized,
        429 to R.string.api_too_many_request,
        500 to R.string.api_server_error
)

data class ClientError(
        @Json(ignored = true)
        var statusCode: Int = 400, // HTTP status code
        override val message: String,
        val error: Reason? = null,
        val code: String? = null,
        val param: String? = null,
        val type: String? = null
) : Exception(message) {

    // Transform statusCode to human readable message.
    // Status 422 is not handled here since every endpoint
    // returns different message.
    // 404 is not handled here too, since its meaning changes
    // for each endpoint.
    fun parseStatusCode(): Int {
        return when (statusCode) {
            // If request header does not contain X-User-Id
            401 -> {
                R.string.api_unauthorized
            }
            429 -> {
                R.string.api_too_many_request
            }
            500 -> {
                R.string.api_server_error
            }
            // All other errors are treated as server error.
            else -> {
                R.string.api_server_error
            }
        }
    }
}


data class Reason(
        val field: String,
        val code: String
) {
    val key: String = "${field}_$code"
}

class NetworkException(msg: String?, cause: Throwable?) : IOException(msg, cause)

class Fetch : AnkoLogger {

    private var method: String = "GET"
    private val headers = Headers.Builder()
    private val reqBuilder = Request.Builder()
    private var reqBody: RequestBody? = null
    private var request: Request? = null
    private var call: Call? = null
    private var timeout: Int = 0

    private var disableCache = false

    fun get(url: String): Fetch {
        reqBuilder.url(url)
        return this
    }

    fun post(url: String): Fetch {
        reqBuilder.url(url)
        method = "POST"
        return this
    }

    fun put(url: String): Fetch {
        reqBuilder.url(url)
        method = "PUT"
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

    fun cancel() {
        call?.cancel()
    }

    // Set timeout in seconds
    fun setTimeout(timeout: Int): Fetch {
        this.timeout = timeout
        return this
    }

    fun getRequest(): Request? {
        return request
    }

    fun header(name: String, value: String): Fetch {
        headers.set(name, value)
        return this
    }

    // Authorization: Bearer xxxxxxx
    private fun setAccessKey(): Fetch {
        headers.set("Authorization", "Bearer ${BuildConfig.ACCESS_TOKEN}")
        return  this
    }

    // X-Client-Type: android
    // X-Client-Version: 2.0.0-google
    fun setClient(): Fetch {
        headers.set("X-Client-Type", "android")
                .set("X-Client-Version", BuildConfig.VERSION_NAME)
        return this
    }

    fun setUserId(uuid: String): Fetch {
        headers.set("X-User-Id", uuid)

        return this
    }

    fun setAppId(): Fetch {
       headers.set("X-App-Id", BuildConfig.WX_SUBS_APPID)
        return this
    }

    fun setUnionId(unionId: String): Fetch {
        headers.set("X-Union-Id", unionId)
        return this
    }

    // Cache-Control: no-cache, no-store, no-transform
    fun noCache(): Fetch {
        disableCache = true
        return this
    }

    /**
     * Use this to send json content.
     */
    fun jsonBody(body: String): Fetch {

        val contentType = MediaType.parse("application/json; charset=utf-8")

        if (contentType == null) {
            headers.set("Content-Type", "application/json; charset=utf-8")
        }

        reqBody = RequestBody.create(contentType, body)

        return this
    }

    /**
     * Use this to transmit binary files.
     */
    fun body(body: ByteArray): Fetch {
        reqBody = RequestBody.create(null, body)

        return this
    }

    /**
     * For POST, PUT, PATCH, PROPPATCH and REPORT method,
     * okhttp does not allow nullable body.
     */
    fun body(): Fetch {
        reqBody = RequestBody.create(null, "")

        return this
    }

    /**
     * Download a file and return the the contents as bytes.
     * If a File is provided as destination, the downloaded content will also saved.
     * Use this to download binary files.
     */
    fun download(): ByteArray? {
        return end().body()?.bytes()
    }

    fun responseString(): String? {
        val resp = end()

        return resp.body()?.string()
    }

    /**
     * Used for next-api and subscription-api.
     * For successful response (HTTP code 200 - 300),
     * return the json string.
     * For client error response (HTTP code > 400)
     */
    fun responseApi(): Pair<Response, String?> {
        setAccessKey()

        /**
         * @throws NetworkException when sending request.
         */
        val resp = end()

        /**
         * Success response.
         * @throws IOException when reading body.
         */
        if (resp.code() in 200 until 400) {
            return Pair(resp, resp.body()?.string())
        }

        /**
         * @throws IOException when turning to string.
         * @throws ClientError
         */
        val body = resp.body()
                ?.string()
                ?: throw ClientError(
                        statusCode = resp.code(),
                        message = resp.message()
                )
        info("API error response: $body")

        // Avoid throwing JSON parse error.
        val clientErr = try {
            Klaxon().parse<ClientError>(body)
        } catch (e: Exception) {
            ClientError(message = resp.message())
        }

        clientErr?.statusCode = resp.code()

        throw clientErr ?: ClientError(
                statusCode = resp.code(),
                message = resp.message()
        )
    }

    /**
     * @return okhttp3.Response
     */
    private fun end(): Response {
        reqBuilder.headers(headers.build())

        if (disableCache) {
            reqBuilder.cacheControl(CacheControl.Builder()
                    .noCache()
                    .noStore()
                    .noTransform()
                    .build())
        }

        /**
         * @throws NullPointerException if method is null, or request url is null.
         * @throws IllegalStateException if method is empty, if body exists for GET method, or if body not exists for POST, PATCH, PUT method.
         */
        val req = reqBuilder
                .method(method, reqBody)
                .build()

        this.request = req

        /**
         * @throws IOException if the request could not be executed due to cancellation, a connectivity
         * problem or timeout. Because networks can fail during an exchange, it is possible that the
         * remote server accepted the request before the failure.
         * @throws IllegalStateException when the call has already been executed.
         */
        val call = if (timeout != 0) {
            client.newBuilder()
                    .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
                    .build()
                    .newCall(req)
        } else {
            client.newCall(req)
        }
        this.call = call

        return try {
            call.execute()
        } catch (e: IOException) {
            // This is used to distinguish network failure from reading body error.
            throw NetworkException(e.message, e.cause)
        }
    }

    companion object {
        private val client = OkHttpClient()
    }
}

