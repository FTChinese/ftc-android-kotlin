package com.ft.ftchinese.model.fetch

import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import okhttp3.Response

data class ServerError(
    override val message: String,
    val error: Unprocessable? = null,

    @Json(ignored = true)
    var statusCode: Int = 400, // HTTP status code

    val code: String? = null,
    val param: String? = null,
    val type: String? = null
) : Exception(message) {

    companion object {
        @JvmStatic
        fun from(resp: Response): ServerError? {
            /**
             * @throws IOException when turning to string.
             */
            val body = resp.body?.string()

            // Avoid throwing JSON parse error.
            val clientErr = if (body != null) {
                /**
                 * Throws JSON parse error.
                 */
                Klaxon().parse<ServerError>(body)
            } else {
                ServerError(
                    message = "No response from server"
                )
            }

            clientErr?.statusCode = resp.code

            return clientErr
        }
    }
}
