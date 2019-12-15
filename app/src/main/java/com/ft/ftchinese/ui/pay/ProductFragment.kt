package com.ft.ftchinese.ui.pay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentProductBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.model.subscription.findPlan
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: ProductViewModel
//    private var product: PaywallProduct? = null
    private lateinit var binding: FragmentProductBinding
    private var tier: Tier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        product = arguments?.getParcelable(ARG_PRODUCT)
        tier = arguments?.getParcelable(ARG_TIER)

//        if (tier != null) {
//            buildProductCard(tier)
//        }
    }

    private fun buildProductCard(): PaywallProduct? {
        val tier = tier ?: return null

        val yearlyPlan = findPlan(tier, Cycle.YEAR)
        val monthlyPlan = findPlan(tier, Cycle.MONTH)

        return PaywallProduct(
                tier = tier,
                heading = when (tier) {
                    Tier.STANDARD -> getString(R.string.tier_standard)
                    Tier.PREMIUM -> getString(R.string.tier_premium)
                },
                description = when (tier) {
                    Tier.STANDARD -> resources
                            .getStringArray(R.array.standard_benefits)
                            .joinToString("\n")
                    Tier.PREMIUM -> resources
                            .getStringArray(R.array.premium_benefits)
                            .joinToString("\n")
                },
                smallPrint = if (tier == Tier.PREMIUM) getString(R.string.premium_small_print) else null,
                yearPrice = getString(R.string.formatter_price_year, yearlyPlan?.amount),
                monthPrice = getString(R.string.formatter_price_month, monthlyPlan?.amount)
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_product, container, false)

//        return inflater.inflate(R.layout.fragment_product, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.product = buildProductCard()

//        product_heading_tv.text = product?.heading
//        product_benefits_tv.text = product?.description

//        if (product?.smallPrint.isNullOrBlank()) {
//            product_small_print.visibility = View.GONE
//        } else {
//            product_small_print.visibility = View.VISIBLE
//        }

//        yearly_price_btn.text = product?.yearPrice

        // When user clicked the price button, find the corresponding Plan
        // and send it to view model so that parent activity could start
        // CheckoutActivity with the selcted Plan.
        binding.yearlyPriceBtn.setOnClickListener {
            val tier = tier ?: return@setOnClickListener
            viewModel.selected.value = findPlan(tier, Cycle.YEAR)
            // Once this button clicked, all price buttons should be disabled.
            viewModel.inputEnabled.value = false
//            it.isEnabled = false
        }

        binding.monthlyPriceBtn.setOnClickListener {
            val tier = tier ?: return@setOnClickListener
            viewModel.selected.value = findPlan(tier, Cycle.MONTH)
            // Once this button clicked, all price buttons should be disabled.
            viewModel.inputEnabled.value = false
//                it.isEnabled = false
        }

//        if (product?.monthPrice.isNullOrBlank()) {
//            monthly_price_btn.visibility = View.GONE
//        } else {
//            monthly_price_btn.text = product?.monthPrice
//
//            monthly_price_btn.setOnClickListener {
//
//            }
//        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProvider(this).get(ProductViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        // Waiting for signal from host activity to enable/disable buttons.
        viewModel.inputEnabled.observe(viewLifecycleOwner, Observer<Boolean> {
//            yearly_price_btn.isEnabled = it
//            monthly_price_btn.isEnabled = it
            product.buttonEnabled = it
        })
    }

    // Buttons are disabled upon click to prevent loading
    // new activity multiple times.
    // When the fragment is resumed, enable those buttons.
    override fun onResume() {
        super.onResume()
        product.buttonEnabled = true
    }

    companion object {
        private const val ARG_TIER = "arg_tier"

        @JvmStatic
        fun newInstance(tier: Tier) = ProductFragment().apply {
            arguments = bundleOf(ARG_TIER to tier)
        }
    }
}

