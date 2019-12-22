package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.R
import kotlinx.android.parcel.Parcelize

@Parcelize
enum class Tier(val symbol: String) : Parcelable {
    STANDARD("standard"),
    PREMIUM("premium");

    val stringRes: Int
        get() =  when (this) {
            STANDARD -> R.string.tier_standard
            PREMIUM -> R.string.tier_premium
        }

    val productDescRes: Int
        get() = when (this) {
            STANDARD -> R.array.standard_benefits
            PREMIUM -> R.array.premium_benefits
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
