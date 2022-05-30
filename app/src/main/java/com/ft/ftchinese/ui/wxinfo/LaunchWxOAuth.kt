package com.ft.ftchinese.ui.wxinfo

import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI

fun launchWxOAuth(
    wxApi: IWXAPI
) {
    val stateCode = WxOAuth.generateStateCode(WxOAuthIntent.LINK)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    wxApi.sendReq(req)
}
