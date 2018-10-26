package com.ft.ftchinese.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

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