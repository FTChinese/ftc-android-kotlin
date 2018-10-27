package com.ft.ftchinese.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.ft.ftchinese.R
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.toast
import java.io.IOException

fun Activity.getActiveNetworkInfo(): NetworkInfo {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return connectivityManager.activeNetworkInfo
}

fun Activity.isNetworkConnected(): Boolean {
    val networkInfo = getActiveNetworkInfo()
    return networkInfo.isConnected
}

fun Activity.isActiveNetworkWifi(): Boolean {
    val networkInfo = getActiveNetworkInfo()

    return when (networkInfo.type) {
        ConnectivityManager.TYPE_WIFI -> true
        else -> false
    }
}

fun Activity.isActiveNetworkMobile(): Boolean {
    val networkInfo = getActiveNetworkInfo()

    return when (networkInfo.type) {
        ConnectivityManager.TYPE_MOBILE,
        ConnectivityManager.TYPE_MOBILE_DUN -> true
        else -> false
    }
}

fun Activity.handleException(e: Exception) {
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