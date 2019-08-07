package com.ft.ftchinese.ui.base

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.ft.ftchinese.R
import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.Tier
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
        val msg = when (resp.error.key) {
            "email_already_exists" -> getString(R.string.api_email_taken)
            "email_invalid" -> getString(R.string.error_invalid_email)
            "password_invalid" -> getString(R.string.error_invalid_password)
            "email_server_missing" -> getString(R.string.api_email_server_down)
            "userName_already_exists" -> getString(R.string.api_name_taken)
            "membership_already_upgraded" -> getString(R.string.api_already_premium)
            else -> resp.message
        }
        toast(msg)
        return
    }

    val msg = when (resp.statusCode) {
        400 -> {
            getString(R.string.api_bad_request)
        }
        // If request header does not contain X-User-Id
        401 -> {
            getString(R.string.api_unauthorized)
        }
        429 -> {
            getString(R.string.api_too_many_request)
        }
        // All other errors are treated as server error.
        else -> {
            getString(R.string.api_server_error)
        }
    }

    toast(msg)
}

fun Activity.handleException(e: Exception) {
    val msg = when (e) {
        is IllegalStateException -> {
            getString(R.string.api_empty_url)
        }
        is NetworkException -> {
            getString(R.string.api_network_failure)
        }
        is IOException -> {
           getString(R.string.api_io_error)
        }
        else -> {
            e.toString()
        }
    }

    toast(msg)
}

fun Activity.parseException(e: Exception): String {
    return when (e) {
        is IllegalStateException -> {
            getString(R.string.api_empty_url)
        }
        is NetworkException -> {
            getString(R.string.api_network_failure)
        }
        is IOException -> {
            getString(R.string.api_io_error)
        }
        else -> {
            e.message ?: getString(R.string.error_unknown)
        }
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

