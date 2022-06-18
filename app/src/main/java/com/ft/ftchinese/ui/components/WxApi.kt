package com.ft.ftchinese.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthKind
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Composable
fun rememberWxApi(
    context: Context = LocalContext.current
): IWXAPI = remember {
    WXAPIFactory.createWXAPI(
        context,
        BuildConfig.WX_SUBS_APPID,
        false
    )
}

fun launchWxLogin(
    wxApi: IWXAPI
) {
    val stateCode = WxOAuth.generateStateCode(WxOAuthKind.LOGIN)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    wxApi.sendReq(req)
}

fun launchWxForLink(
    wxApi: IWXAPI
) {
    val stateCode = WxOAuth.generateStateCode(WxOAuthKind.LINK)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    wxApi.sendReq(req)
}
