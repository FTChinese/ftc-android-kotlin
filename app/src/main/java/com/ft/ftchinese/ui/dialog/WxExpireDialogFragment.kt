package com.ft.ftchinese.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.model.reader.WxOAuth
import com.ft.ftchinese.model.reader.WxOAuthIntent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

@Deprecated("")
class WxExpireDialogFragment : DialogFragment() {

    private lateinit var wxApi: IWXAPI
    private lateinit var sessionManager: SessionManager

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

            /**
             * NOTE: whichever button is clicked, `onDismiss` method
             * will be called. Be cautious performing destructive
             * actions when overriding it.
             */
            MaterialAlertDialogBuilder(it).setMessage(R.string.wx_session_expired)
                    .setPositiveButton(R.string.wx_relogin){ dialog, id ->
                        authorize()
                        // Or should be simply redirects user to
                        // login activity?
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.action_cancel){ dialog, id ->
                        Log.i(TAG, "Cancel button pressed")
                    }
                    .create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun authorize() {
        val nonce = WxOAuth.generateStateCode(WxOAuthIntent.LOGIN)
        Log.i(TAG, "Wechat oauth state: $nonce")

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = nonce

        wxApi.sendReq(req)
    }

    companion object {
        private const val TAG = "WxExpireDialogFragment"
    }
}
