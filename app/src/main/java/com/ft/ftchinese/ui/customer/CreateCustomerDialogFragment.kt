package com.ft.ftchinese.ui.customer

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R
import com.ft.ftchinese.store.AccountCache

/**
 * If user is not a Stripe customer yet, ask whether we should
 * create it using current email.
 */
class CreateCustomerDialogFragment : DialogFragment() {

    private var positiveButtonListener: DialogInterface.OnClickListener? = null
    private var negativeButtonListener: DialogInterface.OnClickListener? = null

    fun onPositiveButtonClicked(listener: DialogInterface.OnClickListener): CreateCustomerDialogFragment {
        this.positiveButtonListener = listener
        return this
    }

    fun onNegativeButtonClicked(listener: DialogInterface.OnClickListener): CreateCustomerDialogFragment {
        this.negativeButtonListener = listener
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle("注册Stripe用户")
                .setMessage("Stripe支付要求提供邮箱地址，是否使用当前邮箱注册(${AccountCache.get()?.email})？")
                .setPositiveButton(R.string.yes, positiveButtonListener)
                .setNegativeButton(R.string.no, negativeButtonListener)
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
