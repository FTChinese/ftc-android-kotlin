package com.ft.ftchinese.model.ftcsubs

import com.tencent.mm.opensdk.modelpay.PayReq
import kotlinx.serialization.Serializable

// This is user's payment intent.
@Serializable
data class WxPayIntent(
    val price: Price,
    val order: Order,
    val params: WxPaySDKParams,
) {
    fun toPayIntent(): FtcPayIntent {
        return FtcPayIntent(
            price = price,
            order = order,
        )
    }
}

@Serializable
data class WxPaySDKParams(
    val app: WxAppPayParams?
)

@Serializable
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
