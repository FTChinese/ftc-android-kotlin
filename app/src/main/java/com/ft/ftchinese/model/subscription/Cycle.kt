package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class Cycle(val symbol: String) : Parcelable {
    MONTH("month"),
    YEAR("year");

    fun string(): String {

        return symbol
    }

    override fun toString(): String {
        return symbol
    }

    companion object {

        private val stringToEnum: Map<String, Cycle> = values().associateBy { it.symbol }

        @JvmStatic
        fun fromString(symbol: String?): Cycle? {
            return stringToEnum[symbol]
        }
    }
}
