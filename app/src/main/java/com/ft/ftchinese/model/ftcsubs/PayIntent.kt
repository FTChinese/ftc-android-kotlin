package com.ft.ftchinese.model.ftcsubs

open class PayIntent(
    open val price: Price,
    open val order: Order,
) {
    fun withConfirmed(o: Order): PayIntent {
        return PayIntent(
            price = price,
            order = o,
        )
    }
}
