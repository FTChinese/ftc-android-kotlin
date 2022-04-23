package com.ft.ftchinese.model.fetch

import com.ft.ftchinese.R

@Deprecated("Use ToastMessage")
sealed class FetchUi {
    data class ResMsg(val strId: Int) : FetchUi() // Id of string resources
    data class TextMsg(val text: String) : FetchUi()
    data class Progress(val loading: Boolean) : FetchUi()

    companion object {
        @JvmStatic
        fun fromApi(e: APIError): FetchUi {
            return when (e.statusCode) {
                401 -> ResMsg(R.string.api_unauthorized)
                429 -> ResMsg(R.string.api_too_many_request)
                500 -> ResMsg(R.string.api_server_error)
                else -> TextMsg(e.message)
            }
        }

        @JvmStatic
        fun fromException(e: Exception): FetchUi {
            return when (e) {
                is IllegalStateException -> {
                    ResMsg(R.string.api_empty_url)
                }
                else -> {
                    TextMsg(e.message ?: "")
                }
            }
        }
    }
}
