package com.ft.ftchinese.user

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import com.ft.ftchinese.R
import org.jetbrains.anko.support.v4.toast

class PaymentFragment : DialogFragment() {
    companion object {
        fun newInstance() = PaymentFragment()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val view = LayoutInflater.from(context)
                .inflate(R.layout.fragment_buy, null)

        // You need to explicitly set a theme for the Buttons.
        // Something wrong with the default theme and dialog buttons becomes transparent
        val builder = AlertDialog.Builder(context!!, R.style.Dialog)

        builder.setTitle("欢迎订阅FT会员服务")
//                .setMessage("会员类型:标准会员\n支付金额¥198.00/年")
                .setView(view)
                .setPositiveButton("确定支付") { dialogInterface, i ->
                    toast("Clicked button $i")
                }


        return builder.create()
    }
}