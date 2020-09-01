package com.ft.ftchinese.repository

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.ft.ftchinese.BuildConfig
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.AnkoLogger
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Unprocessable(
    val field: String,
    val code: String
) {
    val key: String = "${field}_$code"
}

data class ClientError(
    override val message: String,
    val error: Unprocessable? = null,

    @Json(ignored = true)
    var statusCode: Int = 400, // HTTP status code

    val code: String? = null,
    val param: String? = null,
    val type: String? = null
) : Exception(message)

fun parseRespErr(resp: Response): ClientError? {
    /**
     * @throws IOException when turning to string.
     */
    val body = resp.body?.string()

//    info("API error response: $body")

    // Avoid throwing JSON parse error.
    val clientErr = if (body != null) {
        /**
         * Throws JSON parse error.
         */
        Klaxon().parse<ClientError>(body)
    } else {
        ClientError(
            message = "No response from server"
        )
    }

    clientErr?.statusCode = resp.code

    return clientErr
}

/**
 * To use okhttp, you need to build url first using `HttpUrl.Builder`;
 * next build request using `Request.Builder`.
 */
class Fetch : AnkoLogger {

    private var urlBuilder = HttpUrl.Builder()
    private var method: String = "GET"
    private val headers = Headers.Builder()
    private var reqBody: RequestBody? = null
    private var request: Request? = null
    private var call: Call? = null
    private var timeout: Int = 30

    private var disableCache = false

    fun get(url: String) = apply {
        urlBuilder = url.toHttpUrl().newBuilder()
    }

    fun post(url: String) = apply {
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "POST"
    }

    fun put(url: String) = apply {
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "PUT"
    }

    fun patch(url: String) = apply {
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "PATCH"
    }

    fun delete(url: String) = apply {
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "DELETE"
    }

    fun query(name: String, value: String?) = apply {
        urlBuilder.addQueryParameter(name, value)
    }

    fun cancel() {
        call?.cancel()
    }

    // Set timeout in seconds
    // okhttp default timeout is 10 seconds.
    // 0 means no timeout.
    fun setTimeout(timeout: Int) = apply {
        this.timeout = timeout
    }

    fun getRequest(): Request? {
        return request
    }

    fun header(name: String, value: String) = apply {
        headers[name] = value
        return this
    }

    // Authorization: Bearer xxxxxxx
    private fun setAccessKey() = apply {
        headers["Authorization"] = "Bearer ${Config.accessToken}"
    }

    // X-Client-Type: android
    // X-Client-Version: 2.0.0-google
    fun setClient() = apply {
        headers["X-Client-Type"] = "android"
        headers["X-Client-Version"] = BuildConfig.VERSION_NAME
    }

    fun setUserId(uuid: String) = apply {
        headers["X-User-Id"] = uuid
    }

    fun setAppId() = apply {
        headers["X-App-Id"] = BuildConfig.WX_SUBS_APPID
    }

    fun setUnionId(unionId: String) = apply {
        headers["X-Union-Id"] = unionId
    }

    // Cache-Control: no-cache, no-store, no-transform
    fun noCache() = apply {
        disableCache = true
    }

    /**
     * Use this to send json content.
     * @body - the data to send. If omitted, default to `{}`
     * to prevent server returns EOF error.
     */
    fun sendJson(body: String = "{}") = apply {
        val contentType = "application/json; charset=utf-8".toMediaTypeOrNull()

        if (contentType == null) {
            headers["Content-Type"] = "application/json; charset=utf-8"
        }

        reqBody = body.toRequestBody(contentType)
    }

    /**
     * Use this to transmit binary files.
     */
    fun upload(body: ByteArray) = apply {
        headers["Content-Type"] = "application/octet-stream"

        reqBody = body.toRequestBody(null)
    }

    /**
     * For POST, PUT, PATCH, PROPPATCH and REPORT method,
     * okhttp does not allow nullable body.
     */
    fun sendForm(body: String = "") = apply {
        reqBody = body.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
    }

    fun sendText(body: String = "") = apply {
        reqBody = body.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    /**
     * Download a file and return the the contents as bytes.
     * If a File is provided as destination, the downloaded content will also saved.
     * Use this to download binary files.
     */
    fun download(): ByteArray? {
        return end().body?.bytes()
    }

    fun responseString(): String? {
        val resp = end()

        return resp.body?.string()
    }

    /**
     * Used for next-api and subscription-api.
     * For successful response (HTTP code 200 - 300),
     * return the json string.
     * For client error response (HTTP code > 400)
     */
    fun responseApi(): Pair<Response, String?> {
        setAccessKey()
        setClient()

        val resp = end()

        /**
         * Success response.
         * @throws IOException when reading body.
         */
        if (resp.code in 200 until 400) {
            return Pair(resp, resp.body?.string())
        }

        throw parseRespErr(resp) ?: ClientError(
                statusCode = resp.code,
                message = "Unknown error occurred when sending request"
        )
    }

    /**
     * @return okhttp3.Response
     */
    private fun end(): Response {
        val reqBuilder = Request.Builder()
                .url(urlBuilder.build())
                .headers(headers.build())

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
        val call = if (timeout > 0) {
            timeout.toLong().let {
                client.newBuilder()
                    .readTimeout(it, TimeUnit.SECONDS)
                    .build()
                    .newCall(req)
            }
        } else {
            client.newCall(req)
        }

        this.call = call

        return call.execute()
    }

    companion object {
        private val client = OkHttpClient()
    }
}

