package com.ft.ftchinese.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.ft.ftchinese.R
import com.google.gson.JsonSyntaxException
import org.jetbrains.anko.toast
import java.io.IOException

fun Activity.getActiveNetworkInfo(): NetworkInfo? {
    return try {
        // getSystemService() throws IllegalStateException and the returned value is nullable.
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        if (connectivityManager is ConnectivityManager) {
            connectivityManager.activeNetworkInfo
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}



fun Activity.isNetworkConnected(): Boolean {

    return getActiveNetworkInfo()?.isConnected == true
}


fun Activity.isActiveNetworkWifi(): Boolean {

    val networkInfo = getActiveNetworkInfo()

    // Ignore Android deprecation warning. It's very stupid since you cannot actually deprecate it
    // unless you upgrade your min supported sdk, which will in return reduce your supported devices.
    return when (networkInfo?.type) {
        ConnectivityManager.TYPE_WIFI -> true
        else -> false
    }
}

fun Activity.isActiveNetworkMobile(): Boolean {
    val networkInfo = getActiveNetworkInfo()

    return when (networkInfo?.type) {
        ConnectivityManager.TYPE_MOBILE,
        ConnectivityManager.TYPE_MOBILE_DUN -> true
        else -> false
    }
}

// Using API 28. Unfortunately it also requires that you must increase min supported api.
//fun Activity.getConnectivityManager(): ConnectivityManager? {
//    return try {
//        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
//        if (connectivityManager is ConnectivityManager) {
//            return connectivityManager
//        } else {
//            null
//        }
//    } catch (e: Exception) {
//        null
//    }
//}
//
//fun Activity.isUsinigWifi(): Boolean {
//    return try {
//        val connectivityManager = getConnectivityManager()
//        // Call to activeNetwork throws error.
//        // This requires API 23 which we cannot meet.
//        val network = connectivityManager?.activeNetwork
//
//        val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
//
//        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
//
//    } catch (e: Exception) {
//        false
//    }
//}
//
//fun Activity.isUsingCellular(): Boolean {
//    return try {
//        val connectivityManager = getConnectivityManager()
//        val network = connectivityManager?.activeNetwork
//        val networkCapabilities = connectivityManager?.getNetworkCapabilities(network)
//
//        networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
//    } catch (e: Exception) {
//        false
//    }
//}

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