package com.ft.ftchinese.model.fetch

import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.repository.Endpoint
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * To use okhttp, you need to build url first using `HttpUrl.Builder`;
 * next build request using `Request.Builder`.
 */
class Fetch {

    private var urlBuilder = HttpUrl.Builder()
    private var method: String = "GET"
    private val headers = Headers.Builder()
    private var reqBody: RequestBody? = null
    private var request: Request? = null
    private var call: Call? = null
    private var timeout: Int = 30

    private var disableCache = false

    fun get(url: String) = apply {
        Log.i(TAG, "GET $url")
        urlBuilder = url.toHttpUrl().newBuilder()
    }

    fun post(url: String) = apply {
        Log.i(TAG, "GET $url")
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "POST"
    }

    fun put(url: String) = apply {
        Log.i(TAG, "PUT $url")
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "PUT"
    }

    fun patch(url: String) = apply {
        Log.i(TAG, "PATCH $url")
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "PATCH"
    }

    fun delete(url: String) = apply {
        Log.i(TAG, "DELETE $url")
        urlBuilder = url.toHttpUrl().newBuilder()
        method = "DELETE"
    }

    fun addQuery(name: String, value: String?) = apply {
        urlBuilder.addQueryParameter(name, value)
    }

    fun addParams(p: Map<String, String>) = apply {
        for (item in p) {
            urlBuilder.addQueryParameter(item.key, item.value)
        }
    }

    fun setTest(yes: Boolean) = apply {
        if (yes) {
            urlBuilder.addQueryParameter("test", "true")
        }
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

    fun addHeader(name: String, value: String) = apply {

        headers[name] = value
        return this
    }

    fun addHeaders(m: Map<String, String>) = apply {

        for (item in m) {
            headers[item.key] = item.value
        }
    }

    // Authorization: Bearer xxxxxxx
    fun setAccessKey() = apply {
        headers["Authorization"] = "Bearer ${Endpoint.accessToken}"
    }

    private fun setUserAgent(ua: String) = apply {
        headers["User-Agent"] = ua
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
    fun sendJson(body: String = "") = apply {
        val contentType = "application/json; charset=utf-8".toMediaTypeOrNull()

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

    fun endText(): HttpResp<String> {
        val resp = end()

        return HttpResp(
            message = resp.message,
            code = resp.code,
            body = resp.body?.string()
        )
    }

    inline fun <reified T> endJson(withRaw: Boolean = false): HttpResp<T> {
        val resp = end()

        /**
         * @throws IOException when reading body.
         */
        return resp.body?.string()?.let {
            HttpResp(
                message = resp.message,
                code = resp.code,
                body = json.parse<T>(it),
                raw = if (withRaw) {
                    it
                } else {
                    ""
                }
            )
        } ?: HttpResp(
            message = resp.message,
            code = resp.code,
            body = null,
            raw = "",
        )
    }

    /**
     * NOTE: this only parse json object, not array.
     */
    inline fun <reified T> endApiJson(withRaw: Boolean = false): HttpResp<T> {
        setAccessKey()

        val resp = end()

        /**
         * Success response.
         * @throws IOException when reading body.
         */
        if (resp.code in 200 until 400) {
            return resp.body?.string()?.let {
                HttpResp(
                    message = resp.message,
                    code = resp.code,
                    body = json.parse<T>(it),
                    raw = if (withRaw) {
                        it
                    } else {
                        ""
                    }
                )
            } ?: HttpResp(
                message = resp.message,
                code = resp.code,
                body = null,
                raw = "",
            )
        }

        throw APIError.from(resp)
    }

    inline fun <reified T> endApiArray(withRaw: Boolean = false): HttpResp<List<T>> {
        setAccessKey()

        val resp = end()

        /**
         * Success response.
         * @throws IOException when reading body.
         */
        if (resp.code in 200 until 400) {
            return resp.body?.string()?.let {
                HttpResp(
                    message = resp.message,
                    code = resp.code,
                    body = json.parseArray(it),
                    raw = if (withRaw) {
                        it
                    } else {
                        ""
                    }
                )
            } ?: HttpResp(
                message = resp.message,
                code = resp.code,
                body = null,
                raw = "",
            )
        }

        throw APIError.from(resp)
    }

    fun endApiText(): HttpResp<String> {
        setAccessKey()

        val resp = end()

        /**
         * Success response.
         * @throws IOException when reading body.
         */
        if (resp.code in 200 until 400) {
            return HttpResp(
                message = resp.message,
                code = resp.code,
                body = resp.body?.string()
            )
        }

        throw APIError.from(resp)
    }

    /**
     * @return okhttp3.Response
     */
    fun end(): Response {
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
         * @throws IllegalStateException
         * if method is empty,
         * if body exists for GET method, or
         * if body not exists for POST, PATCH, PUT method.
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
        const val TAG = "Fetch"
        private val client = OkHttpClient()
    }
}

