package com.ft.ftchinese.ui.wxlink

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.ft.ftchinese.store.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

/**
 * Show a wechat authorization dialog for a ftc-only user
 */
class LinkWxDialogFragment : DialogFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var wxApi: IWXAPI

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            MaterialAlertDialogBuilder(it)
                .setMessage("尚未关联微信。绑定微信账号后，可以使用微信账号账号快速登录")
                .setPositiveButton("微信授权"){ dialog, id ->
                    linkWechat()
                    // Or should be simply redirects user to
                    // login activity?
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.action_cancel){ dialog, id ->
                    info("Cancel button pressed")
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    /**
     * Launch Wechat OAuth workflow to request a code from wechat.
     * It will jump to wxapi.WXEntryActivity.
     */
    private fun linkWechat() {
        val stateCode = WxOAuth.generateStateCode(WxOAuthIntent.LINK)

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = stateCode

        wxApi.sendReq(req)
    }
}
