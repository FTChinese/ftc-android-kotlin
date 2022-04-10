package com.ft.ftchinese.model.ftcsubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.DiscountStatus
import com.ft.ftchinese.model.enums.OfferKind
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Parcelize
@Serializable
data class Discount(
    val id: String = "",
    val currency: String = "cny",
    val description: String? = null,
    val kind: OfferKind? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    val overridePeriod: YearMonthDay = YearMonthDay(), // Override the period specified in price if present.
    val priceOff: Double? = null,
    val percent: Int? = null,
    val priceId: String = "",
    val recurring: Boolean = false,
    val liveMode: Boolean = false,
    val status: DiscountStatus? = null,
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

    fun isIntroductory(): Boolean {
        return kind == OfferKind.Introductory
    }
}
