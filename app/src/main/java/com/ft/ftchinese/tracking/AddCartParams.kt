package com.ft.ftchinese.tracking

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PriceKind
import com.ft.ftchinese.model.ftcsubs.Price
import com.ft.ftchinese.model.stripesubs.StripePrice

data class AddCartParams(
    val id: String,
    val name: String,
    val category: String,
    val edition: Edition,
) {
    companion object {
        @JvmStatic
        fun ofFtc(price: Price): AddCartParams {
            val cycle = price.periodCount.toCycle()

            return AddCartParams(
                id = price.id,
                name = price.tier.toString(),
                category = if (price.kind == PriceKind.OneTime) {
                    "introductory"
                } else {
                    cycle.toString()
                },
                edition = Edition(
                    tier = price.tier,
                    cycle = price.periodCount.toCycle()
                )
            )
        }

        @JvmStatic
        fun ofStripe(price: StripePrice): AddCartParams {
            val cycle = price.periodCount.toCycle()

            return AddCartParams(
                id = price.id,
                name = price.tier.toString(),
                category = cycle.toString(),
                edition = Edition(
                    tier = price.tier,
                    cycle = cycle,
                )
            )
        }
    }
}
