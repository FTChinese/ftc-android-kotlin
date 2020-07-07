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
import com.ft.ftchinese.model.subscription.Cycle
import com.ft.ftchinese.model.subscription.Tier
import com.ft.ftchinese.model.subscription.findPlan
import com.ft.ftchinese.viewmodel.ProductViewModel
import org.jetbrains.anko.AnkoLogger

/**
 * Show a card of product.
 * Hosted insidie [PaywallActivity] or [UpgradeActivity].
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: ProductViewModel
    private lateinit var binding: FragmentProductBinding
    private var tier: Tier? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tier = arguments?.getParcelable(ARG_TIER)

//        if (tier != null) {
//            buildProductCard(tier)
//        }
    }

    private fun buildProductCard(): UIPaywallProduct? {
        val tier = tier ?: return null

        val yearlyPlan = findPlan(tier, Cycle.YEAR)
        val monthlyPlan = findPlan(tier, Cycle.MONTH)

        return UIPaywallProduct(
                tier = tier,
                heading = getString(tier.stringRes),
                description =resources
                        .getStringArray(tier.productDescRes)
                        .joinToString("\n"),
                smallPrint = if (tier == Tier.PREMIUM) getString(R.string.premium_small_print) else null,
                yearPrice = getString(R.string.formatter_price_year, yearlyPlan?.amount),
                monthPrice = if (monthlyPlan != null) {
                    getString(R.string.formatter_price_month, monthlyPlan.amount)
                } else { null }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_product, container, false)

        binding.product = buildProductCard()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        }

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
            binding.buttonEnabled = it
        })
    }

    // Buttons are disabled upon click to prevent loading
    // new activity multiple times.
    // When the fragment is resumed, enable those buttons.
    override fun onResume() {
        super.onResume()
        binding.buttonEnabled = true
    }

    companion object {
        private const val ARG_TIER = "arg_tier"

        @JvmStatic
        fun newInstance(tier: Tier) = ProductFragment().apply {
            arguments = bundleOf(ARG_TIER to tier)
        }
    }
}

