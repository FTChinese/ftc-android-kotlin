package com.ft.ftchinese.model.enums

import android.os.Parcelable
import com.ft.ftchinese.R
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.threeten.bp.Period

@Parcelize
@Serializable
enum class Cycle(val symbol: String) : Parcelable {
    @SerialName("month")
    MONTH("month"),
    @SerialName("year")
    YEAR("year");

    val stringRes: Int
        get() = when (this) {
            MONTH -> R.string.cycle_month
            YEAR -> R.string.cycle_year
        }

    // The temporal amount to be added to existing membership.
    val period: Period
        get() = when (this) {
            MONTH -> Period.of(0, 1, 1)
            YEAR -> Period.of(1, 0, 1)
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
