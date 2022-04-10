package com.ft.ftchinese.model.fetch

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Response

// API error.
@Serializable
data class APIError(
    override val message: String,
    val error: Unprocessable? = null,

    // Not from API
    @Transient
    var statusCode: Int = 400, // HTTP status code

    // Stripe Only
    val code: String? = null,
    val param: String? = null,
    val type: String? = null
) : Exception(message) {

    companion object {

        /**
         * Throws JSON parse error.
         * @throws IOException when reading body
         */
        @JvmStatic
        fun from(resp: Response): APIError {
            /**
             *
             */
            val body = resp.body?.string() ?: return APIError(
                message = "No response from server",
                statusCode = resp.code
            )

            // In case of ngix error, it might be HTML
            if (body.startsWith("<")) {
                return APIError(
                    message = body,
                    statusCode = 500
                )
            }


            return Json.decodeFromString<APIError>(body).apply {
                statusCode = resp.code
            }
        }
    }
}
