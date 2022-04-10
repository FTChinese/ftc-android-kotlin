package com.ft.ftchinese.model.enums

import android.os.Parcelable
import com.ft.ftchinese.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class Tier(val symbol: String) : Parcelable {
    @SerialName("standard")
    STANDARD("standard"),
    @SerialName("premium")
    PREMIUM("premium");

    val stringRes: Int
        get() =  when (this) {
            STANDARD -> R.string.tier_standard
            PREMIUM -> R.string.tier_premium
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
