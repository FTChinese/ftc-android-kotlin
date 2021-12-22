package com.ft.ftchinese.tracking

import com.ft.ftchinese.model.enums.Edition
import com.ft.ftchinese.model.enums.PriceKind
import com.ft.ftchinese.model.ftcsubs.Price

data class CartParams(
    val id: String,
    val name: String,
    val category: String,
    val edition: Edition,
) {
    companion object {
        @JvmStatic
        fun ofFtc(price: Price): CartParams {
            val cycle = price.periodCount.toCycle()

            return CartParams(
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
    }
}
