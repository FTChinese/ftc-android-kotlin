package com.ft.ftchinese.model.fetch

import com.ft.ftchinese.R

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class FetchResult<out T : Any> {

    data class Success<out T : Any>(val data: T) : FetchResult<T>()
    data class LocalizedError(val msgId: Int) : FetchResult<Nothing>()
    data class TextError(val text: String) : FetchResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is LocalizedError -> "LocalizedError[msgId=$msgId"
            is TextError -> "Error[exception=$text]"
        }
    }

    companion object {
        val loadingFailed = LocalizedError(R.string.loading_failed)
        val notConnected = LocalizedError(R.string.prompt_no_network)
        val unknownError = LocalizedError(R.string.error_unknown)
        val accountNotFound = LocalizedError(R.string.account_not_found)

        @JvmStatic
        fun fromApi(e: APIError): FetchResult<Nothing> {
            return when (e.statusCode) {
                401 -> LocalizedError(R.string.api_unauthorized)
                429 -> LocalizedError(R.string.api_too_many_request)
                500 -> LocalizedError(R.string.api_server_error)
                else -> TextError(e.message)
            }
        }

        @JvmStatic
        fun fromException(e: Exception): FetchResult<Nothing> {
            return when (e) {
                is IllegalStateException -> {
                    LocalizedError(R.string.api_empty_url)
                }
                else -> {
                    TextError(e.message ?: "")
                }
            }
        }
    }
}
