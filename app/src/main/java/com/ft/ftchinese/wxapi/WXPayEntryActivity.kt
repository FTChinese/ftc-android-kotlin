package com.ft.ftchinese.wxapi

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class WXPayEntryActivity: Activity(), IWXAPIEventHandler, AnkoLogger {
    private var api: IWXAPI? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.pay_result)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID)

        api?.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        api?.handleIntent(intent, this)
    }

    override fun onResp(resp: BaseResp?) {
        info("onPayFinish, errCode = ${resp?.errCode}")

        if (resp?.type == ConstantsAPI.COMMAND_PAY_BY_WX) {
            when (resp?.errCode) {
                0 -> {
                    toast("Payment done")
                    // Query order
                }
                -1 -> {
                    toast("Payment error")
                }
                -2 -> {
                    toast("User cancelled")
                }
            }
        }
    }

    override fun onReq(req: BaseReq?) {

    }
}