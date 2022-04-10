package com.ft.ftchinese.model.ftcsubs

import kotlinx.serialization.Serializable

@Serializable
data class AliPayIntent(
    val price: Price,
    val order: Order,
    val params: AliPaySDKParams,
) {
    fun toPayIntent(): FtcPayIntent {
        return FtcPayIntent(
            price = price,
            order = order,
        )
    }
}

@Serializable
data class AliPaySDKParams(
    val app: String?
)
