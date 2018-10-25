package com.ft.ftchinese.models

import com.google.gson.annotations.SerializedName

data class ErrorResponse (
        var statusCode: Int, // HTTP status code
        override val message: String,
        val error: ErrorDetail
) : Exception(message)

/**
 * Mind naming conflict with Kotlin.
 * `field` identifier is used by Kotlin as backing fields.
 */
data class ErrorDetail(
        @SerializedName("field") val errField: String,
        @SerializedName("code") val errCode: String
) {
    val msgKey: String
        get() = "${errField}_$errCode"
}