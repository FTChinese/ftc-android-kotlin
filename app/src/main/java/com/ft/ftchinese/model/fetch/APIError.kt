package com.ft.ftchinese.model.fetch

import com.beust.klaxon.Json
import okhttp3.Response

// API error.
data class APIError(
    override val message: String,
    @Json(ignored = true)
    var statusCode: Int = 400, // HTTP status code

    val error: Unprocessable? = null,

    val code: String? = null,
    val param: String? = null,
    val type: String? = null
) : Exception(message) {

    companion object {
        @JvmStatic
        fun from(resp: Response): APIError {
            /**
             * @throws IOException when turning to string.
             */
            val body = resp.body?.string()

            // Avoid throwing JSON parse error.
            if (body != null) {
                // Might not return json body
                if (body.startsWith("<")) {
                    return APIError(
                        message = body,
                        statusCode = 500
                    )
                }

                /**
                 * Throws JSON parse error.
                 */
                return try {
                    json.parse<APIError>(body)?.apply {
                        statusCode = resp.code
                    } ?: APIError(
                            message = "Error parsing JSON",
                            statusCode = 500
                        )
                } catch (e: Exception) {
                    APIError(
                        message = e.message ?: "Error parsing JSON",
                        statusCode = 500
                    )
                }
            }

            return  APIError(
                message = "No response from server",
                statusCode = resp.code
            )
        }
    }
}
