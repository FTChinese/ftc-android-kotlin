package com.ft.ftchinese.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.ft.ftchinese.BuildConfig
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class WXEntryActivity : Activity(), IWXAPIEventHandler, AnkoLogger {

    lateinit var api: IWXAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID, false)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        this.intent = intent

        api.handleIntent(intent, this)
    }

    override fun onResp(resp: BaseResp?) {

        toast("Response type: ${resp?.type}")

        val result = when (resp?.errCode) {
            BaseResp.ErrCode.ERR_OK -> "Success"
            BaseResp.ErrCode.ERR_USER_CANCEL -> "User cancelled"
            BaseResp.ErrCode.ERR_AUTH_DENIED -> "Authentication denied"
            BaseResp.ErrCode.ERR_UNSUPPORT -> "Unsupported"
            else -> "Unknown"
        }

        toast(result)
    }

    override fun onReq(req: BaseReq?) {
        when (req?.type) {
            ConstantsAPI.COMMAND_GETMESSAGE_FROM_WX -> {

            }
            ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX -> {

            }
        }
    }

}