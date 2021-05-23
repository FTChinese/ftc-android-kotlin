package com.ft.ftchinese.model.fetch

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import okhttp3.Response
import org.jetbrains.anko.AnkoLogger

data class ServerError(
    override val message: String,
    val error: Unprocessable? = null,

    @Json(ignored = true)
    var statusCode: Int = 400, // HTTP status code

    val code: String? = null,
    val param: String? = null,
    val type: String? = null
) : Exception(message), AnkoLogger {

    companion object {
        @JvmStatic
        fun from(resp: Response): ServerError {
            /**
             * @throws IOException when turning to string.
             */
            val body = resp.body?.string()

            // Avoid throwing JSON parse error.
            if (body != null) {
                // Might not return json body
                if (body.startsWith("<")) {
                    return ServerError(
                        message = body,
                        statusCode = 500
                    )
                }

                /**
                 * Throws JSON parse error.
                 */
                return try {
                    Klaxon().parse<ServerError>(body)?.apply {
                        statusCode = resp.code
                    } ?: ServerError(
                            message = "Kalxon.parse error",
                            statusCode = 500
                        )
                } catch (e: Exception) {
                    ServerError(
                        message = e.message ?: "Error parsing JSON",
                        statusCode = 500
                    )
                }
            }

            return  ServerError(
                message = "No response from server",
                statusCode = resp.code
            )
        }
    }
}
