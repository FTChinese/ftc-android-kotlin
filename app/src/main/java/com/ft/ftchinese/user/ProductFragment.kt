package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.util.getPriceCycleText
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.fragment_product.*
import org.jetbrains.anko.AnkoLogger

class ProductFragment : Fragment(), AnkoLogger {

    private var tier: Tier? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var listener: OnSelectProductListener? = null

    interface OnSelectProductListener {
        fun onSelectProduct()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnSelectProductListener) {
            listener = context
        }

        sessionManager = SessionManager.getInstance(context)
        firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tier = Tier.fromString(arguments?.getString(ARG_TIER))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (tier) {
            Tier.STANDARD -> updateUIForStandard()
            Tier.PREMIUM -> updateUIForPremium()
        }
    }

    private fun updateUIForStandard() {
        product_heading_tv.text = getString(R.string.tier_standard)
        product_benefits_tv.text = resources
                .getStringArray(R.array.standard_benefits)
                .joinToString("\n" +
                        "✔ ")

        product_small_print.visibility = View.GONE

        yearly_price_btn.text = activity?.getPriceCycleText(Tier.STANDARD, Cycle.YEAR)
        monthly_price_btn.text = activity?.getPriceCycleText(Tier.STANDARD, Cycle.MONTH)


        yearly_price_btn.setOnClickListener {
            val account = sessionManager.loadAccount()
            if (account == null) {
                CredentialsActivity.startForResult(activity)
                return@setOnClickListener
            }

            listener?.onSelectProduct()

            PaymentActivity.startForResult(
                    activity = activity,
                    requestCode = RequestCode.PAYMENT,
                    tier = Tier.STANDARD,
                    cycle = Cycle.YEAR
            )
        }

        monthly_price_btn.setOnClickListener {
            val account = sessionManager.loadAccount()

            if (account == null) {
                CredentialsActivity.startForResult(activity)
                return@setOnClickListener
            }

            listener?.onSelectProduct()

            PaymentActivity.startForResult(
                    activity = activity,
                    requestCode = RequestCode.PAYMENT,
                    tier = Tier.STANDARD,
                    cycle = Cycle.MONTH
            )
        }
    }

    private fun updateUIForPremium() {
        product_heading_tv.text = getString(R.string.tier_premium)
        product_benefits_tv.text = resources
                .getStringArray(R.array.premium_benefits)
                .joinToString("\n" +
                        "✔ ")

        product_small_print.text = getString(R.string.premium_small_print)

        yearly_price_btn.text = activity?.getPriceCycleText(Tier.PREMIUM, Cycle.YEAR)
        monthly_price_btn.visibility = View.GONE



        yearly_price_btn.setOnClickListener {
            val account = sessionManager.loadAccount()

            if (account == null) {
                CredentialsActivity.startForResult(activity)
                return@setOnClickListener
            }

            listener?.onSelectProduct()

            PaymentActivity.startForResult(
                        activity = activity,
                        requestCode = RequestCode.PAYMENT,
                        tier = Tier.PREMIUM,
                        cycle = Cycle.YEAR
                )
        }
    }


    companion object {
        private const val ARG_TIER = "arg_tier"
        fun newInstance(tier: Tier) = ProductFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TIER, tier.string())
            }
        }
    }

}