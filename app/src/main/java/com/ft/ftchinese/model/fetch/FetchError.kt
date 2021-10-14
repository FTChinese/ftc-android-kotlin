package com.ft.ftchinese.model.fetch

import com.ft.ftchinese.R

sealed class FetchError {
    data class ResourceId(val resId: Int): FetchError()
    data class PlainText(val message: String): FetchError()

    companion object {
        @JvmStatic
        fun fromServerError(e: APIError): FetchError {
            return when (e.statusCode) {
                401 -> ResourceId(R.string.api_unauthorized)
                429 -> ResourceId(R.string.api_too_many_request)
                500 -> ResourceId(R.string.api_server_error)
                else -> PlainText(e.message)
            }
        }

        @JvmStatic
        fun fromException(e: Exception): FetchError {
            return when (e) {
                is IllegalStateException -> {
                    ResourceId(R.string.api_empty_url)
                }
                else -> {
                    PlainText(e.message ?: "")
                }
            }
        }
    }
}
