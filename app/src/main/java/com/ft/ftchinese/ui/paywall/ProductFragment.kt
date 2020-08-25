package com.ft.ftchinese.ui.paywall

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
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.ui.base.ScopedFragment
import org.jetbrains.anko.AnkoLogger
import org.threeten.bp.format.DateTimeFormatter

/**
 * Show a card of product.
 * Hosted inside [PaywallActivity] or [UpgradeActivity].
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: ProductViewModel
    private lateinit var binding: FragmentProductBinding
    private var tier: Tier? = null
    private val plans = mutableMapOf<Cycle, Plan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tier = arguments?.getParcelable(ARG_TIER)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_product, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // When user clicked the price button, find the corresponding Plan
        // and send it to view model so that parent activity could start
        // CheckoutActivity with the selcted Plan.
        binding.yearlyPriceBtn.setOnClickListener {
            val plan = plans[Cycle.YEAR]

            viewModel.selected.value = plan

            // Once this button clicked, all price buttons should be disabled.
            viewModel.inputEnabled.value = false
        }

        binding.monthlyPriceBtn.setOnClickListener {
            val plan = plans[Cycle.MONTH]

            viewModel.selected.value = plan

            // Once this button clicked, all price buttons should be disabled.
            viewModel.inputEnabled.value = false
        }

        initProduct()
        initPrice(defaultPlans)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Init view model
        viewModel = activity?.run {
            ViewModelProvider(this).get(ProductViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        // Enable or disable button
        viewModel.inputEnabled.observe(viewLifecycleOwner, Observer<Boolean> {
            binding.buttonEnabled = it
        })

        // Observing products
        viewModel.plansReceived.observe(viewLifecycleOwner, Observer<List<Plan>> {
            initPrice(it)
        })
    }

    private fun initProduct() {
        val tier = tier ?: return

        binding.product =  UIProduct(
            heading = getString(tier.stringRes),
            description =resources
                    .getStringArray(tier.productDescRes)
                    .joinToString("\n"),
            smallPrint = if (tier == Tier.PREMIUM) {
                getString(R.string.premium_small_print)
            } else {
                null
            }
        )
    }

    private fun initPrice(plans: List<Plan>) {
        plans
            .filter { it.tier == tier }
            .forEach { plan ->

                this.plans[plan.cycle] = plan

                when (plan.cycle) {
                    Cycle.YEAR -> binding.yearPrice = buildPrice(plan)
                    Cycle.MONTH -> binding.monthPrice = buildPrice(plan)
                }
            }
    }

    private fun buildPrice(plan: Plan): Price {

        val cycleStr = getString(plan.cycle.stringRes)

        return Price(
            amount = getString(
                R.string.formatter_price_cycle,
                plan.payableAmount(),
                cycleStr
            ),

            discountPeriod = if (plan.discount.isValid()) {
                 getString(
                     R.string.discount_period,
                     plan.discount
                         .endUtc
                         ?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                )

            } else {
                null
            },

            originalPrice = getString(R.string.original_price) + getString(
                R.string.formatter_price_cycle,
                plan.price,
                cycleStr
            )
        )
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

