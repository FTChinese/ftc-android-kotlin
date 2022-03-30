package com.ft.ftchinese.ui.base

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * https://developer.android.com/training/basics/network-ops/reading-network-state
 * getSystemService(ConnectivityManager::class.java)
 */
val Context.isConnected: Boolean
    get() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager
                ?.activeNetwork
                ?: return false

            val netCap = connectivityManager
                .getNetworkCapabilities(networkCapabilities)
                ?: return false

            return when {
                netCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                netCap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                netCap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            val netInfo = connectivityManager
                ?.activeNetworkInfo
                ?: return false

            return when (netInfo.type) {
                ConnectivityManager.TYPE_WIFI -> true
                ConnectivityManager.TYPE_MOBILE -> true
                ConnectivityManager.TYPE_ETHERNET -> true
                else -> false
            }
        }
    }



