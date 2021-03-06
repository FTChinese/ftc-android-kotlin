package com.ft.ftchinese.model.fetch

import com.ft.ftchinese.R

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class FetchResult<out T : Any> {

    data class Success<out T : Any>(val data: T) : FetchResult<T>()
    data class LocalizedError(val msgId: Int) : FetchResult<Nothing>()
    data class Error(val exception: Exception) : FetchResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is LocalizedError -> "LocalizedError[msgId=$msgId"
            is Error -> "Error[exception=$exception]"
        }
    }

    companion object {
        @JvmStatic
        fun fromServerError(e: ServerError): FetchResult<Nothing> {
            return when (e.statusCode) {
                401 -> LocalizedError(R.string.api_unauthorized)
                429 -> LocalizedError(R.string.api_too_many_request)
                else -> Error(e)
            }
        }

        fun fromException(e: Exception): FetchResult<Nothing> {
            return when (e) {
                is IllegalStateException -> {
                    LocalizedError(R.string.api_empty_url)
                }
                else -> {
                    Error(e)
                }
            }
        }
    }
}
