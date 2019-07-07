package com.ft.ftchinese.model.order

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class Tier(val symbol: String) : Parcelable {
    STANDARD("standard"),
    PREMIUM("premium");

    fun string(): String {
        return symbol
    }

    override fun toString(): String {
        return symbol
    }

    companion object {

        private val stringToEnum: Map<String, Tier> = values().associateBy {
            it.symbol
        }

        @JvmStatic
        fun fromString(symbol: String?): Tier? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
