package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWxLoginBinding
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * Wechat login UI.
 * It just calls the Wechat SDK and the rest operations are
 * delegated to wxapi.WXEntryActivity
 */
class WxLoginFragment : Fragment() {

    private var wxApi: IWXAPI? = null
    private lateinit var binding: FragmentWxLoginBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_wx_login, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.handler = this
    }

    fun onClickOAuth(view: View) {
        view.isEnabled = false

        val nonce = WxOAuth.generateStateCode(WxOAuthIntent.LOGIN)
        Log.i(TAG, "Wechat oauth state: $nonce")

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = nonce

        // DO NOT FORGET to call this!
        wxApi?.sendReq(req)
        activity?.finish()
    }

    companion object {
        private const val TAG = "WxLoginFragment"
        @JvmStatic
        fun newInstance() = WxLoginFragment()
    }
}
