package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.BuildConfig

import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_link_wx.*


class LinkWxFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private var wxApi: IWXAPI? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_link_wx, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Start WXEntryActivity and finish the parent
         * [WxInfoActivity]
         */
        btn_wx_authorization.setOnClickListener {
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
                LinkWxFragment()
    }
}
