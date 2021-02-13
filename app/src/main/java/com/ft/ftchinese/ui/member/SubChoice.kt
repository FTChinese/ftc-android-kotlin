package com.ft.ftchinese.ui.member

import com.ft.ftchinese.model.enums.OrderKind
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.Tier
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.subscription.*

data class SubChoice(
    val orderKind: OrderKind,
    val price: Price,
    val discount: Discount? = null,
    val smallPrint: String = "",
) {
    companion object {
        @JvmStatic
        fun fromFtcPrice(m: Membership, p: Plan): SubChoice {
            val orderKind = when {
                m.tier == p.tier -> OrderKind.Renew
                p.tier == Tier.PREMIUM -> OrderKind.Upgrade
                p.tier == Tier.STANDARD -> OrderKind.AddOn
                else -> OrderKind.Create
            }

            return SubChoice(
                orderKind = orderKind,
                price = p.unifiedPrice,
                discount = if (p.promotionOffer.isValid()) {
                    p.promotionOffer
                } else null
            )
        }
    }
}
