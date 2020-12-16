package com.ft.ftchinese.model.subscription

import android.os.Parcelable
import com.ft.ftchinese.util.KDateTime
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class Discount(
    val id: String? = null,
    val priceOff: Double? = null,
    val percent: Int? = null,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null,
    val description: String? = null
) : Parcelable {

    // isValid checks whether a discount exists and whehter it is in valid period.
    fun isValid(): Boolean {
        if (priceOff == null || priceOff <= 0) {
            return false
        }

        val now = ZonedDateTime.now()

        if (now.isBefore(startUtc) || now.isAfter(endUtc)) {
            return false
        }

        return true
    }
}
