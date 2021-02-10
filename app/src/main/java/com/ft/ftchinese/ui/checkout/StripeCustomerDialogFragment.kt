package com.ft.ftchinese.ui.checkout

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R
import com.ft.ftchinese.store.SessionManager

class StripeCustomerDialogFragment : DialogFragment() {
    private lateinit var sessionManager: SessionManager

    private var positiveButtonListener: DialogInterface.OnClickListener? = null
    private var negativeButtonListener: DialogInterface.OnClickListener? = null

    fun onPositiveButtonClicked(listener: DialogInterface.OnClickListener): StripeCustomerDialogFragment {
        this.positiveButtonListener = listener
        return this
    }

    fun onNegativeButtonClicked(listener: DialogInterface.OnClickListener): StripeCustomerDialogFragment {
        this.negativeButtonListener = listener
        return this
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle("注册Stripe用户")
                .setMessage("Stripe支付要求提供邮箱地址，是否使用当前邮箱注册(${sessionManager.loadAccount()?.email})？")
                .setPositiveButton(R.string.yes, positiveButtonListener)
                .setNegativeButton(R.string.no, negativeButtonListener)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
