package com.ft.ftchinese.wxapi

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityWechatBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
open class WxBaseActivity : ScopedAppActivity() {

    protected var api: IWXAPI? = null
    protected lateinit var sessionManager: SessionManager
    protected lateinit var binding: ActivityWechatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_wechat)
        setSupportActionBar(binding.toolbar.toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        sessionManager = SessionManager.getInstance(this)
    }
}
