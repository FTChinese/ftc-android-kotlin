package com.ft.ftchinese.model.ftcsubs

data class AliPayIntent(
    override val price: Price,
    override val order: Order,
    val param: AliPaySDKParams,
) : PayIntent(
    price = price,
    order = order,
)

data class AliPaySDKParams(
    val appSdk: String?
)
