package com.ft.ftchinese.model.price

import android.os.Parcelable
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KOfferKind
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class Discount(
    val id: String? = null,
    val currency: String = "cny",
    val priceOff: Double? = null,
    val percent: Int? = null,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null,
    val description: String? = null,
    @KOfferKind
    val kind: OfferKind? = null,
) : Parcelable {

    // isValid checks whether a discount exists and whether it is in valid period.
    fun isValid(): Boolean {
        if (priceOff == null || priceOff <= 0) {
            return false
        }

        if (startUtc == null || endUtc == null) {
            return true
        }

        val now = ZonedDateTime.now()

        if (now.isBefore(startUtc) || now.isAfter(endUtc)) {
            return false
        }

        return true
    }
}
