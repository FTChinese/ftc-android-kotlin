package com.ft.ftchinese.ui.components

import com.ft.ftchinese.R
import com.ft.ftchinese.model.fetch.APIError

sealed class ToastMessage {
    data class Resource(val id: Int) : ToastMessage()
    data class Text(val text: String) : ToastMessage()

    companion object {
        val errorUnknown = Resource(R.string.error_unknown)

        @JvmStatic
        fun fromApi(e: APIError): ToastMessage {
            return when (e.statusCode) {
                401 -> Resource(R.string.api_unauthorized)
                429 -> Resource(R.string.api_too_many_request)
                500 -> Resource(R.string.api_server_error)
                else -> Text(e.message)
            }
        }

        @JvmStatic
        fun fromException(e: Exception): ToastMessage {
            return when (e) {
                is IllegalStateException -> {
                    Resource(R.string.api_empty_url)
                }
                else -> {
                    Text(e.message ?: "")
                }
            }
        }
    }
}
