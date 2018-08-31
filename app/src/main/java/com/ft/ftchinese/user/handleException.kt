package com.ft.ftchinese.user

import android.support.v4.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.util.EmptyResponseException
import com.ft.ftchinese.util.NetworkException
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.support.v4.toast
import java.io.IOException

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