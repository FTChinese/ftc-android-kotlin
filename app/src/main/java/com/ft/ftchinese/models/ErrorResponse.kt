package com.ft.ftchinese.models

data class ErrorResponse (
        var statusCode: Int,
        override val message: String,
        val error: ErrorDetail
) : Exception(message) {

    override fun toString(): String {
        return "API error message: $message, field: ${error.field}, code: ${error.code}"
    }
}

data class ErrorDetail(
        val field: String,
        val code: String
) {
    companion object {
        const val CODE_MISSING = "missing"
        const val CODE_MISSING_FIELD = "missing_field"
        const val CODE_INVALID = "invalid"
        const val ALREADY_EXISTS = "already_exists"
    }
}