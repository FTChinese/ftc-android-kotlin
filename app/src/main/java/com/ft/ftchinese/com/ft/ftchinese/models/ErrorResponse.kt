package com.ft.ftchinese.com.ft.ftchinese.models

data class ErrorResponse(
        val message: String,
        val error: ErrorDetail
)

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