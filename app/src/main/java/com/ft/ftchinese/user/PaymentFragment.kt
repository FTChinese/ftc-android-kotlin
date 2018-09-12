package com.ft.ftchinese.user

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Membership
import org.jetbrains.anko.support.v4.toast


class PaymentFragment : DialogFragment() {
    companion object {
        private const val ARG_MEMBERSHIP_TYPE = "member_type"
        fun newInstance(type: String) = PaymentFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MEMBERSHIP_TYPE, type)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val memberType = arguments?.getString(ARG_MEMBERSHIP_TYPE) ?: Membership.TYPE_STANDARD

        val membership = Membership(type = memberType)

        val view = LayoutInflater.from(context)
                .inflate(R.layout.fragment_payment, null)

        val memberTypeValue = view.findViewById<TextView>(R.id.value_member_type)
        val memberPriceValue = view.findViewById<TextView>(R.id.value_member_price)
        val payZhifubao = view.findViewById<ImageView>(R.id.pay_zhifubao)
        val payWechat = view.findViewById<ImageView>(R.id.pay_wechat)

        memberTypeValue.text = getString(membership.typeResId)
        memberPriceValue.text = getString(membership.priceResId)

        payZhifubao.setOnClickListener {
            toast("Pay ${membership.price} by zhifubao")
            dialog.dismiss()
        }

        payWechat.setOnClickListener {
            toast("Pay ${membership.price} by wechat")
            dialog.dismiss()
        }

        // You need to explicitly set a theme for the Buttons.
        // Something wrong with the default theme and dialog buttons becomes transparent
        val builder = AlertDialog.Builder(context!!, R.style.Dialog)

        builder.setTitle(R.string.pay_dialog_title)
                .setView(view)
                .setNegativeButton(R.string.pay_cancel) { _, _ ->
                    this.dialog.cancel()
                }


        return builder.create()
    }
}