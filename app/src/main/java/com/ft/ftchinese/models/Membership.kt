package com.ft.ftchinese.models

import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.R
import kotlinx.coroutines.experimental.async
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat


data class Membership(
        val type: String = "free",
        val startAt: String? = null,
        // ISO8601 format. Example: 2019-08-05T07:19:41Z
        val expireAt: String? = null
) {
    /**
     * Compare expireAt against now.
     */
    val isExpired: Boolean
        get() {
            val expire = expireAt ?: return true

            return DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis()).isBeforeNow
        }

    val typeResId: Int
        get() = when(type) {
            TYPE_PREMIUM -> R.string.member_type_premium
            else -> R.string.member_type_standard
        }

    val priceResId: Int
        get() = when(type) {
            TYPE_PREMIUM -> R.string.price_annual_premium
            else -> R.string.price_annual_standard
        }

    val price: Int
        get() = when(type) {
            TYPE_PREMIUM -> PRICE_PREMIUM
            else -> PRICE_STANDARD
        }

    // 2019-08-06
    val localizedExpireDate: String?
        get() {
            val expire = expireAt ?: return null
            val dateTime = DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis())
            return ISODateTimeFormat.date().print(dateTime)
        }

    fun requestOrderAsync() = async {

    }

    companion object {
        const val TYPE_FREE = "free"
        const val TYPE_STANDARD = "standard"
        const val TYPE_PREMIUM = "premium"
        const val PRICE_STANDARD = 198
        const val PRICE_PREMIUM = 1998
    }
}

data class Subscription(
        val price: Int,
        val channel: Int
) {
    companion object {
        const val PRICE_STANDARD = 198
        const val PRICE_PREMIUM = 1998
        const val VIA_ALIPAY = 1
        const val VIA_WECHAT = 2
    }
}