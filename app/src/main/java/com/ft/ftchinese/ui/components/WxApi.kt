package com.ft.ftchinese.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthKind
import com.ft.ftchinese.wxapi.WxLoginDiagnostic
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

private const val TAG = "WxApi"

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
    wxApi: IWXAPI,
    context: Context
) {
    wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
    val stateCode = WxOAuth.generateStateCode(WxOAuthKind.LOGIN)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    val sent = wxApi.sendReq(req)
    WxLoginDiagnostic.recordLaunch(
        context = context,
        kind = WxOAuthKind.LOGIN.name,
        state = stateCode,
        sent = sent
    )
    Log.i(TAG, "Launch wx login request sent=$sent")
}

fun launchWxForLink(
    wxApi: IWXAPI
) {
    wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
    val stateCode = WxOAuth.generateStateCode(WxOAuthKind.LINK)

    val req = SendAuth.Req()
    req.scope = WxOAuth.SCOPE
    req.state = stateCode

    val sent = wxApi.sendReq(req)
    Log.i(TAG, "Launch wx link request sent=$sent")
}
