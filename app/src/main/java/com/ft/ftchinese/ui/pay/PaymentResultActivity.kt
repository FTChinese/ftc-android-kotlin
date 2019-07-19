package com.ft.ftchinese.ui.pay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import kotlinx.android.synthetic.main.activity_payment_result.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class PaymentResultActivity : ScopedAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_result)

        val result = intent.getParcelableExtra<PaymentOutcome>(EXTRA_PAYMENT_RESULT) ?: return

        invoice_number_tv.text = getString(R.string.outcome_invoice_number, result.invoice)
        plan_tv.text = getString(R.string.outcome_plan)
        period_tv.text = getString(R.string.outcome_period)
        sub_status_tv.text = getString(R.string.outcome_sub_status)
        payment_status_tv.text = getString(R.string.outcome_payment_status)

        done_button.setOnClickListener {
            MemberActivity.start(this)
        }
    }

    companion object {

        private const val EXTRA_PAYMENT_RESULT = "extra_payment_result"

        @JvmStatic
        fun start(context: Context, outcome: PaymentOutcome) {
            context.startActivity(Intent(context, PaymentResultActivity::class.java).apply {
                putExtra(EXTRA_PAYMENT_RESULT, outcome)
            })
        }
    }
}
