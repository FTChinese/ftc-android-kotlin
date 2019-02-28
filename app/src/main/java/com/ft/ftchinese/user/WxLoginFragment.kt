package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.WxOAuth
import com.ft.ftchinese.models.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_wx_login.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WxLoginFragment : Fragment(), AnkoLogger {

    private var wxApi: IWXAPI? = null
    private var sessionManager: SessionManager? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context != null) {
            sessionManager = SessionManager.getInstance(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_wx_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wechat_oauth_btn.setOnClickListener {
            val nonce = WxOAuth.stateCode()
            info("Wechat oauth state: $nonce")

            sessionManager?.saveWxState(nonce)
            sessionManager?.saveWxIntent(WxOAuthIntent.LOGIN)

            val req = SendAuth.Req()
            req.scope = WxOAuth.SCOPE
            req.state = nonce

            // DO NOT FORGET to call this!
            wxApi?.sendReq(req)

            activity?.finish()
        }
    }

    companion object {
        fun newInstance() = WxLoginFragment()
    }
}