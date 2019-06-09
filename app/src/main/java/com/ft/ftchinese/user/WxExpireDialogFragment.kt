package com.ft.ftchinese.user

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.WxOAuth
import com.ft.ftchinese.model.WxOAuthIntent
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class WxExpireDialogFragment : DialogFragment(), AnkoLogger {

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
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it, R.style.DialogTheme)
            /**
             * NOTE: whichever button is clicked, `onDismiss` method
             * will be called. Be cautious performing destructive
             * actions when overriding it.
             *
             */
            builder.setMessage(R.string.wx_session_expired)
                    .setPositiveButton(R.string.wx_relogin){ dialog, id ->
                        authorize()
                        // Or should be simply redirects user to
                        // login activity?
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.action_cancel){ dialog, id ->
                        info("Cancel button pressed")
                    }

            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun authorize() {
        val nonce = WxOAuth.stateCode()
        info("Wechat oauth state: $nonce")

        sessionManager.saveWxState(nonce)
        sessionManager.saveWxIntent(WxOAuthIntent.LOGIN)

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = nonce

        wxApi.sendReq(req)
    }
}
