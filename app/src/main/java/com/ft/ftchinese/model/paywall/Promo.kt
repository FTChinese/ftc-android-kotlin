package com.ft.ftchinese.model.paywall

import com.ft.ftchinese.model.fetch.KDateTime
import org.threeten.bp.ZonedDateTime

data class Promo(
    val id: String? = null,
    val heading: String? = null,
    val subHeading: String? = null,
    val coverUrl: String? = null,
    val content: String? = null,
    val terms: String? = null,
    @KDateTime
    val startUtc: ZonedDateTime?,
    @KDateTime
    val endUtc: ZonedDateTime?
) {
    fun isValid(): Boolean {
        if (id == null) {
            return false
        }

        val now = ZonedDateTime.now()

        return !now.isBefore(startUtc) && !now.isAfter(endUtc)
    }
}
