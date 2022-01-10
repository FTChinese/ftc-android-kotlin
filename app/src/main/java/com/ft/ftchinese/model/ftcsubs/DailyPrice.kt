package com.ft.ftchinese.model.ftcsubs

/**
 * DailyPrice is used to replace placeholder in product description.
 */
data class DailyPrice(
    val holder: String,
    val replacer: String,
) {
    companion object {
        @JvmStatic
        fun formatMoney(amount: Double): String {
            return "%.2f".format(amount)
        }

        @JvmStatic
        fun ofYear(avg: Double): DailyPrice {
            return DailyPrice(
                holder = "{{dailyAverageOfYear}}",
                replacer = formatMoney(avg),
            )
        }

        @JvmStatic
        fun ofMonth(avg: Double): DailyPrice {
            return DailyPrice(
                holder = "{{dailyAverageOfMonth}}",
                replacer = formatMoney(avg),
            )
        }
    }
}
