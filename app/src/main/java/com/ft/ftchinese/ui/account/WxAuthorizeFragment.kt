package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.BuildConfig

import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWxAuthorizeBinding
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * Show a wechat authorization button for a ftc-only user
 * when click the link-wechat item in [AccountActivity].
 * This fragment is host inside [WxInfoActivity] which attaches
 * this one is user is not linked to wechat yet or [WxInfoFragment]
 * if user already linked.
 */
class WxAuthorizeFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var wxApi: IWXAPI? = null
    private lateinit var binding: FragmentWxAuthorizeBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_wx_authorize, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Start WXEntryActivity and finish the parent
         * [WxInfoActivity]
         */
        binding.btnWxAuthorization.setOnClickListener {
            linkWechat()
            activity?.finish()
        }
    }

    /**
     * Launch Wechat OAuth workflow to request a code from wechat.
     * It will jump to wxapi.WXEntryActivity.
     */
    private fun linkWechat() {
        val stateCode = WxOAuth.stateCode()

        sessionManager.saveWxState(stateCode)
        sessionManager.saveWxIntent(WxOAuthIntent.LINK)

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = stateCode

        wxApi?.sendReq(req)
    }



    companion object {
        @JvmStatic
        fun newInstance() =
                WxAuthorizeFragment()
    }
}
