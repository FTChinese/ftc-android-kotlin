package com.ft.ftchinese.ui.checkout

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import java.lang.IllegalStateException

/**
 * Urge wechat user to link to email so that we could create
 * Stripe customer.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailRequiredDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle("绑定邮箱")
                .setMessage("您当前使用了微信登录。Stripe支付要求提供邮箱，是否绑定邮箱？")
                .setPositiveButton(R.string.yes) { _, _ ->
                    LinkFtcActivity.startForResult(activity)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    dismiss()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
