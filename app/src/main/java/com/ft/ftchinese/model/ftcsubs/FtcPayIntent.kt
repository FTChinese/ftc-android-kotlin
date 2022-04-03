package com.ft.ftchinese.model.ftcsubs

open class FtcPayIntent(
    open val price: Price,
    open val order: Order,
) {
    fun withConfirmed(o: Order): FtcPayIntent {
        return FtcPayIntent(
            price = price,
            order = o,
        )
    }
}
