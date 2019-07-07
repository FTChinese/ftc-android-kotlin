package com.ft.ftchinese.model.order

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class OrderUsage(val code: String) : Parcelable {
    CREATE("create"),
    RENEW("renew"),
    UPGRADE("upgrade");

    override fun toString(): String {
        return code
    }

    companion object{

        private val stringToEnum: Map<String, OrderUsage> = values().associateBy {
            it.code
        }

        @JvmStatic
        fun fromString(symbol: String): OrderUsage? {
            return stringToEnum[symbol]
        }
    }
}
