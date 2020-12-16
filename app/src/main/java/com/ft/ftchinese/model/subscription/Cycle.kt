package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Cycle(val symbol: String) : Parcelable {
    MONTH("month"),
    YEAR("year");

    val stringRes: Int
        get() = when (this) {
            MONTH -> R.string.cycle_month
            YEAR -> R.string.cycle_year
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
