package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KTier
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class PriceMetadata(
    @KTier
    val tier: Tier,
    val periodDays: Int = 0,
    val introductory: Boolean = false,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null
) : Parcelable {
    fun isValid(): Boolean {
        if (periodDays <= 0) {
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
