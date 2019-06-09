package com.ft.ftchinese.base

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.ft.ftchinese.R
import com.ft.ftchinese.model.*
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.NetworkException
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

/**
 * Generate human readable membership type.
 * 标准会员/年
 * 标准会员/月
 * 高级会员/年
 */
fun Activity.getTierCycleText(tier: Tier?, cycle: Cycle?): String? {

    val tierText = when (tier) {
        Tier.STANDARD -> getString(R.string.tier_standard)
        Tier.PREMIUM -> getString(R.string.tier_premium)
        else -> return null
    }

    val cycleText = when (cycle) {
        Cycle.YEAR -> getString(R.string.cycle_year)
        Cycle.MONTH -> getString(R.string.cycle_month)
        else -> return null
    }

    return getString(R.string.formatter_tier_cycle, tierText, cycleText)
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

fun Activity.handleApiError(resp: ClientError) {
    // Here handles 422 response.
    // Currently only 422's response has `error` field.
    if (resp.error != null) {
        when (resp.error.key) {
            "email_already_exists" -> toast(R.string.api_email_taken)
            "email_invalid" -> toast(R.string.error_invalid_email)
            "password_invalid" -> toast(R.string.error_invalid_password)
            "email_server_missing" -> toast(R.string.api_email_server_down)
            "userName_already_exists" -> toast(R.string.api_name_taken)
            "membership_already_upgraded" -> toast(R.string.api_already_premium)
            else -> toast(resp.message)
        }
        return
    }

    when (resp.statusCode) {
        400 -> {
            toast(R.string.api_bad_request)
        }
        // If request header does not contain X-User-Id
        401 -> {
            toast(R.string.api_unauthorized)
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

fun Activity.handleException(e: Exception) {
    e.printStackTrace()
    when (e) {
        is IllegalStateException -> {
            toast(R.string.api_empty_url)
        }
        is NetworkException -> {
            toast(R.string.api_network_failure)
        }
        is IOException -> {
            toast(R.string.api_io_error)
        }
        else -> {
            toast(e.toString())
        }
    }
}
