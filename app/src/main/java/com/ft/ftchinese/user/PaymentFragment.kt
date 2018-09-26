package com.ft.ftchinese.user

import android.app.Activity
import android.app.Dialog
import android.content.Intent
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
        const val ARG_MEMBERSHIP_TIER = "member_tier"
        const val ARG_BILLING_CYCLE = "billing_cycle"
        const val EXTRA_PAYMENT_METHOD = "payment_method"

        fun newInstance(tier: String, billingCycle: String) = PaymentFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MEMBERSHIP_TIER, tier)
                putString(ARG_BILLING_CYCLE, billingCycle)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val memberTier = arguments?.getString(ARG_MEMBERSHIP_TIER) ?: Membership.TIER_STANDARD
        val billingCycle = arguments?.getString(ARG_BILLING_CYCLE) ?: Membership.BILLING_YEARLY

        val membership = Membership(tier = memberTier, billingCycle = billingCycle)

        val view = LayoutInflater.from(context)
                .inflate(R.layout.fragment_payment, null)

        val memberTierValue = view.findViewById<TextView>(R.id.value_member_tier)
        val memberPriceValue = view.findViewById<TextView>(R.id.value_member_price)
        val alipayBtn = view.findViewById<ImageView>(R.id.pay_alipay)
        val tenpayBtn = view.findViewById<ImageView>(R.id.pay_tenpay)

        memberTierValue.text = getString(membership.tierResId)
        memberPriceValue.text = getString(membership.priceResId)

        alipayBtn.setOnClickListener {
            toast("Pay ${membership.price} by zhifubao")
//            dialog.dismiss()
            sendResult(Activity.RESULT_OK, memberTier, billingCycle, Membership.PAYMENT_METHOD_ALI)
        }

        tenpayBtn.setOnClickListener {
            toast("Pay ${membership.price} by wechat")
//            dialog.dismiss()
            sendResult(Activity.RESULT_OK, memberTier, billingCycle, Membership.PAYMENT_METHOD_WX)
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

    private fun sendResult(resultCode: Int, memberTier: String, billingCycle: String, paymentMethod: Int) {
        if (targetFragment == null) {
            return
        }

        val intent = Intent()
        intent.putExtra(ARG_MEMBERSHIP_TIER, memberTier)
        intent.putExtra(ARG_BILLING_CYCLE, billingCycle)
        intent.putExtra(EXTRA_PAYMENT_METHOD, paymentMethod)

        targetFragment?.onActivityResult(targetRequestCode, resultCode, intent)
    }
}