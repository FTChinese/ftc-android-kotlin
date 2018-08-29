package com.ft.ftchinese.util

import android.util.Log
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.models.ErrorResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.Request
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import java.io.IOException

val gson = Gson()

class Fetch : AnkoLogger {

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

    /**
     * @return String if request successfully, or null otherwise.
     * @throws IllegalStateException If url is empty
     * @throws IOException If request cannot be executed, or response body cannot be read
     */
    fun string(): String? {
        reqBuilder.headers(headers.build())

        if (cacheControl != null) {
            reqBuilder.cacheControl(cacheControl!!)
        }

        val response = client.newCall(reqBuilder.build()).execute()

        info("Reponse code: ${response.code()}. Message: ${response.message()}")

        if (response.isSuccessful) {
            return response.body()?.string()
        }

        return null
    }

    /**
     * @return Response
     * See execute for thrown errors.
     */
    fun end(): Response {
        reqBuilder.headers(headers.build())

        if (cacheControl != null) {
            reqBuilder.cacheControl(cacheControl!!)
        }

        reqBuilder.method(method, reqBody)

        return execute(reqBuilder)
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

        info("URL: ${request.url()}. Method: ${request.method()}. Headers: ${request.headers()}")

        val response = client.newCall(builder.build()).execute()


        info("Reponse code: ${response.code()}. Message: ${response.message()}")

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

    companion object {
        private val client = OkHttpClient()
    }
}