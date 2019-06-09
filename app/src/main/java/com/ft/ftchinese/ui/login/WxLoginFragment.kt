package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.WxOAuth
import com.ft.ftchinese.model.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_wx_login.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WxLoginFragment : Fragment(), AnkoLogger {

    private var wxApi: IWXAPI? = null
    private var sessionManager: SessionManager? = null
    lateinit var viewModel: LoginViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_wx_login, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        wechat_oauth_btn.setOnClickListener {
            authorize()
        }
    }

    private fun authorize() {
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

    companion object {
        @JvmStatic
        fun newInstance() = WxLoginFragment()
    }
}
