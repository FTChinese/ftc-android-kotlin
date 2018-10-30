package com.ft.ftchinese.user

import android.support.v4.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.util.EmptyResponseException
import com.ft.ftchinese.util.NetworkException
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.support.v4.toast
import java.io.IOException

const val FIELD_EMAIL = "email"
const val FIELD_PASSWORD = "password"
const val CODE_MISSING = "missing"
const val CODE_MISSING_FIELD = "missing_field"
const val CODE_INVALID = "invalid"
const val CODE_ALREADY_EXISTS = "already_exists"

/**
 * Maps api 422 response to string resource id.
 */
val apiErrResId = mapOf<String, Int>(
        "email_already_exists" to R.string.api_email_taken,
        "email_invalid" to R.string.error_invalid_email,
        "password_invalid" to R.string.error_invalid_password,
        "email_server_missing" to R.string.api_email_server_down
)

fun Fragment.handleException(e: Exception) {
    e.printStackTrace()
    when (e) {
        is IllegalStateException -> {
            toast(R.string.api_empty_url)
        }
        is NetworkException -> {
            toast(R.string.api_network_failure)
        }
        is EmptyResponseException -> {
            toast(R.string.api_empty_response)
        }
        is IOException -> {
            toast(R.string.api_io_error)
        }
        is JsonSyntaxException -> {
            toast(R.string.api_json_syntax)
        }
        else -> {
            toast(e.toString())
        }
    }
}

/**
 * Handle api error response.
 * This is used to handle common errors.
 * Many request has specified error message to show that could only be handled in its own fragment.
 */
fun Fragment.handleApiError(resp: ErrorResponse) {
    when (resp.statusCode) {
        400 -> {
            toast(R.string.api_bad_request)
        }
        // If request header does not contain X-User-Id
        401 -> {
            toast(R.string.api_unauthorized)
        }
        422 -> {
            val resId = apiErrResId[resp.error.msgKey]
            if (resId != null) {
                toast(resId)
            }
        }
        429 -> {
            toast(R.string.api_too_many_request)
        }
        // All other errors are treated as server error.
        else -> {
            toast(R.string.api_server_error)
        }
    }
}