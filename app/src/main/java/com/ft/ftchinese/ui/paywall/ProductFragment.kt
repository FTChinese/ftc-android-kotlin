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

/**
 * Show a card of product.
 * Hosted inside [PaywallActivity] or [UpgradeActivity].
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: ProductViewModel
    private lateinit var binding: FragmentProductBinding

    /**
     * The tier of current product.
     */
    private var tier: Tier? = null

    /**
     * The pricing plans for current product is stored as a map so that
     * when use clicked we can find out which price is clicked and tells
     * host activity what to do next.
     */
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

        /**
         * When user clicked the price button, find the corresponding Plan
         * and send it to view model so that parent activity could start
         * CheckoutActivity with the selcted Plan.
         */
        binding.yearlyPriceBtn.setOnClickListener {
            val plan = plans[Cycle.YEAR]

            viewModel.selected.value = plan

            /**
             * Disable all buttons.
             */
            viewModel.inputEnabled.value = false
        }

        binding.monthlyPriceBtn.setOnClickListener {
            val plan = plans[Cycle.MONTH]

            viewModel.selected.value = plan

            /**
             * Once this button clicked, all price buttons should be disabled.
             */
            viewModel.inputEnabled.value = false
        }

        /**
         * The data for product UI is retrieve from embedded string resources so that there won't be any lag when drawing UI.
         * We don't use data from since it won't change frequently and the content actually does not matter much.
         */
        initProduct()
        //
        /**
         * Initially we use the hard-coded pricing plans.
         * Then we'll load paywall data from cache,
         * then fetch latest data from server.
         */
        initPrice(defaultPlans.filter { it.tier == tier })
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
        viewModel.productsReceived.observe(viewLifecycleOwner, Observer<List<Product>> { products ->
            products.find {
                it.tier == tier
            }?.let {
                initPrice(it.plans)
            }

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
        plans.forEach { plan ->

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

            originalPrice = if (plan.discount.isValid()) {
                getString(R.string.original_price) + getString(
                    R.string.formatter_price_cycle,
                    plan.price,
                    cycleStr
                )
            } else {
                null
            }

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

