package com.ft.ftchinese.model.price

import android.os.Parcelable
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KDiscountStatus
import com.ft.ftchinese.model.fetch.KOfferKind
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class Discount(
    val id: String = "",
    val currency: String = "cny",
    val description: String? = null,
    @KOfferKind
    val kind: OfferKind? = null,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null,
    val priceOff: Double? = null,
    val percent: Int? = null,
    val priceId: String = "",
    val recurring: Boolean = false,
    val liveMode: Boolean = false,
    @KDiscountStatus
    val status: DiscountStatus? = null,
) : Parcelable {

    // isValid checks whether a discount exists and whether it is in valid period.
    fun isValid(): Boolean {
        if (BuildConfig.DEBUG)
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
