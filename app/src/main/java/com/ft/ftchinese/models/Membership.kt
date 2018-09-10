package com.ft.ftchinese.models

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


    // 2019-08-06
    val localizedExpireDate: String?
        get() {
            val expire = expireAt ?: return null
            val dateTime = DateTime.parse(expire, ISODateTimeFormat.dateTimeNoMillis())
            return ISODateTimeFormat.date().print(dateTime)
        }

    companion object {
        const val TYPE_FREE = "free"
        const val TYPE_STANDARD = "standard"
        const val TYPE_PREMIUM = "premium"
    }
}