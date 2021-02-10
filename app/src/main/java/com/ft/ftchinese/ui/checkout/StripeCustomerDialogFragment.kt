package com.ft.ftchinese.ui.checkout

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.viewmodel.CustomerViewModel
import org.jetbrains.anko.internals.AnkoInternals.createAnkoContext
import java.lang.IllegalStateException

class StripeCustomerDialogFragment : DialogFragment() {
    private lateinit var sessionManager: SessionManager
    private lateinit var customerViewModel: CustomerViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
        customerViewModel = ViewModelProvider(this)
            .get(CustomerViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            AlertDialog.Builder(it)
                .setTitle("注册Stripe用户")
                .setMessage("Stripe支付要求提供邮箱地址，是否使用当前邮箱注册(${sessionManager.loadAccount()?.email})？\n该邮箱将用于接收Stripe的发票等信息.")
                .setPositiveButton("注册") { _, _ ->
                    // Test host activity that yest button is clicked.
                    customerViewModel.creatingCustomer.value = true
                    dismiss()
                }
                .setNegativeButton("取消") { _, _ ->
                    dismiss()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
