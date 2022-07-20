package com.ft.ftchinese.model.stripesubs

import com.ft.ftchinese.model.enums.DiscountStatus
import com.ft.ftchinese.model.paywall.MoneyParts
import com.ft.ftchinese.model.paywall.convertCent
import com.ft.ftchinese.model.paywall.getCurrencySymbol
import com.ft.ftchinese.model.serializer.DateTimeAsStringSerializer
import kotlinx.serialization.Serializable
import org.threeten.bp.ZonedDateTime

@Serializable
data class StripeCoupon(
    val id: String,
    val amountOff: Int,
    val currency: String,
    val redeemBy: Long,
    val priceId: String?,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val startUtc: ZonedDateTime? = null,
    @Serializable(with = DateTimeAsStringSerializer::class)
    val endUtc: ZonedDateTime? = null,
    val status: DiscountStatus? = null,
) {
    fun isValid(): Boolean {
        if (startUtc == null || endUtc == null) {
            return amountOff > 0
        }

        if (status != DiscountStatus.Active) {
            return false
        }

        val now = ZonedDateTime.now()

        if (now.isBefore(startUtc) || now.isAfter(endUtc)) {
            return false
        }

        return true
    }

    val moneyParts: MoneyParts
        get() = MoneyParts(
            symbol = getCurrencySymbol(currency),
            amount = convertCent(amountOff),
        )
}
