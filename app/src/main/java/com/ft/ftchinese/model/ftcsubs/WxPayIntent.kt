package com.ft.ftchinese.model.ftcsubs

import com.tencent.mm.opensdk.modelpay.PayReq

// This is user's payment intent.
data class WxPayIntent(
    override val price: Price,
    override val order: Order,
    val params: WxPaySDKParams,
) : PayIntent(
    price = price,
    order = order
)

data class WxPaySDKParams(
    val app: WxAppPayParams?
)

data class WxAppPayParams(
    val appId: String,
    val partnerId: String,
    val prepayId: String,
    val timestamp: String,
    val nonce: String,
    val pkg: String,
    val signature: String,
) {
    fun buildReq(): PayReq {
        val req = PayReq()
        req.appId = appId
        req.partnerId = partnerId
        req.prepayId = prepayId
        req.nonceStr = nonce
        req.timeStamp = timestamp
        req.packageValue = pkg
        req.sign = signature

        return req
    }
}
