package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWxLoginBinding
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.viewmodel.LoginViewModel
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * This fragment shows a Wechat login button inside [LoginActivity] and handles the login process.
 * It just calls the Wechat SDK and the rest operations are
 * delegated to wxapi.WXEntryActivity
 */
class WxLoginFragment : Fragment(), AnkoLogger {

    private var wxApi: IWXAPI? = null
    private var sessionManager: SessionManager? = null
    lateinit var viewModel: LoginViewModel
    private lateinit var binding: FragmentWxLoginBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_wx_login, container, false)

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        binding.wechatOauthBtn.setOnClickListener {
            viewModel.inProgress.value = true
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
