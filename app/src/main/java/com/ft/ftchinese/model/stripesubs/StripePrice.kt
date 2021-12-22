package com.ft.ftchinese.model.stripesubs

import android.os.Parcelable
import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PriceKind
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.fetch.KDateTime
import com.ft.ftchinese.model.fetch.KPriceKind
import com.ft.ftchinese.model.fetch.KTier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import kotlinx.parcelize.Parcelize
import org.threeten.bp.ZonedDateTime

@Parcelize
data class StripePrice(
    val id: String,
    val active: Boolean,
    val currency: String,
    val isIntroductory: Boolean = false,
    @KPriceKind
    val kind: PriceKind = PriceKind.Recurring,
    val liveMode: Boolean,
    val nickname: String,
    val productId: String = "",
    val periodCount: YearMonthDay = YearMonthDay(),
    @KTier
    val tier: Tier,
    val unitAmount: Int,
    @KDateTime
    val startUtc: ZonedDateTime? = null,
    @KDateTime
    val endUtc: ZonedDateTime? = null,
    val created: Int = 0,
) : Parcelable {

    val edition: Edition
        get() = Edition(
            tier = tier,
            cycle = periodCount.toCycle()
        )
    fun isValid(): Boolean {
        if (kind == PriceKind.Recurring) {
            return true
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
    val moneyAmount: Double
        get() = unitAmount
            .toBigDecimal()
            .divide(
                100.toBigDecimal()
            )
            .toDouble()

}
