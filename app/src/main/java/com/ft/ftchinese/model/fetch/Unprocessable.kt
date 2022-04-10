package com.ft.ftchinese.model.fetch

import kotlinx.serialization.Serializable

private const val codeMissing = "missing"
private const val codeMissingField = "missing_field"
private const val codeAlreadyExists = "already_exists"
private const val codeInvalid = "invalid"

@Serializable
data class Unprocessable(
    val field: String,
    val code: String
) {
    val key: String = "${field}_$code"

    val isCodeMissing: Boolean
        get() = code == codeMissing

    val isCodeFieldMissing: Boolean
        get() = code == codeMissingField

    val isCodeAlreadyExists: Boolean
        get() = code == codeAlreadyExists

    val isCodeInvalid: Boolean
        get() = code == codeInvalid

    fun isResourceMissing(f: String): Boolean {
        return field == f && code == codeMissing
    }

    fun isFieldMissing(f: String): Boolean {
        return field == f && code == codeMissingField
    }
    fun isFieldAlreadyExists(f: String): Boolean {
        return field == f && code == codeAlreadyExists
    }

    fun isFieldInvalid(f: String): Boolean {
        return field == f && code == codeInvalid
    }
}
