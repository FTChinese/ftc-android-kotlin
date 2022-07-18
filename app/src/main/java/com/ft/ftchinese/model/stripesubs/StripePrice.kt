package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PriceKind
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.ftcsubs.YearMonthDay
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class StripePrice(
    val id: String,
    val active: Boolean,
    val currency: String,
    val isIntroductory: Boolean = false,
    val kind: PriceKind = PriceKind.Recurring,
    val liveMode: Boolean,
    val nickname: String,
    val productId: String = "",
    val periodCount: YearMonthDay = YearMonthDay(),
    val tier: Tier,
    val unitAmount: Int,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    val created: Int = 0,
) {

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

}
