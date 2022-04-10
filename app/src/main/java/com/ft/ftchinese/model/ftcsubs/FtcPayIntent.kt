package com.ft.ftchinese.model.ftcsubs

class FtcPayIntent(
    val price: Price,
    val order: Order,
) {
    fun withConfirmed(o: Order): FtcPayIntent {
        return FtcPayIntent(
            price = price,
            order = o,
        )
    }
}
