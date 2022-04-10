package com.ft.ftchinese.model.enums

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
enum class PriceKind(val symbol: String) : Parcelable {
    @SerialName("recurring")
    Recurring("recurring"),
    @SerialName("one_time")
    OneTime("one_time");

    override fun toString(): String {
        return symbol
    }

    companion object {
        private val stringToEnum: Map<String, PriceKind> = values().associateBy {
            it.symbol
        }

        @JvmStatic
        fun fromString(symbol: String?): PriceKind? {
            return if (symbol == null) null else stringToEnum[symbol]
        }
    }
}
