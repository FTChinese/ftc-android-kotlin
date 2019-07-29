package com.ft.ftchinese.ui.pay

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.fragment_stripe_outcome.*

// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_STRIPE_OUTCOME = "stripe_outcome"

class StripeOutcomeFragment : Fragment() {

    private var outcome: StripeOutcome? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            outcome = it.getParcelable(ARG_STRIPE_OUTCOME)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stripe_outcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        invoice_number_tv.text = getString(R.string.outcome_invoice_number, outcome?.invoice)
        sub_status_tv.text = getString(R.string.outcome_sub_status, outcome?.subStatus)
        payment_status_tv.text = getString(R.string.outcome_payment_status, outcome?.subStatus)
        period_tv.text = getString(R.string.outcome_period, outcome?.period)
    }

    companion object {

        @JvmStatic
        fun newInstance(outcome: StripeOutcome) =
                StripeOutcomeFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(ARG_STRIPE_OUTCOME, outcome)
                    }
                }
    }
}
