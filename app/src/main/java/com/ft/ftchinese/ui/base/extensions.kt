package com.ft.ftchinese.ui.base

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.ft.ftchinese.R
import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier

val Context.isConnected: Boolean
    get() = (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnected == true

fun getCurrencySymbol(currency: String): String {
    return when (currency) {
        "cny" -> "¥"
        "usd" -> "$"
        "gbp" -> "£"
        else -> currency.toUpperCase()
    }
}

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

/**
 * Generate human readable membership type.
 * 标准会员/年
 * 标准会员/月
 * 高级会员/年
 */
fun Activity.getTierCycleText(tier: Tier?, cycle: Cycle?): String? {

    if (tier == null || cycle == null) {
        return null
    }

    return getString(R.string.formatter_tier_cycle, getString(tier.stringRes), getString(cycle.stringRes))
}

fun formatTierCycle(ctx: Context, tier: Tier?, cycle: Cycle?): String {
    if (tier == null || cycle == null) {
        return ""
    }

    return ctx.getString(
        R.string.formatter_tier_cycle,
        ctx.getString(tier.stringRes),
        ctx.getString(cycle.stringRes)
    )
}

fun Activity.formatPrice(currency: String?, price: Double?): String {
    if (currency == null || price == null) {
        return ""
    }

    return getString(
            R.string.formatter_price,
            getCurrencySymbol(currency),
            price
    )
}

fun formatPrice(ctx: Context, currency: String?, price: Double?): String {
    if (currency == null || price == null) {
        return ""
    }

    return ctx.getString(
        R.string.formatter_price,
        getCurrencySymbol(currency),
        price
    )
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

