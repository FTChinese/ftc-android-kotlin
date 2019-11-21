package com.ft.ftchinese.model

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Result<out T : Any> {

    data class Success<out T : Any>(val data: T) : Result<T>()
    data class LocalizedError(val msgId: Int) : Result<Nothing>()
    data class Error(val exception: Exception) : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is LocalizedError -> "LocalizedError[msgId=$msgId"
            is Error -> "Error[exception=$exception]"
        }
    }
}
