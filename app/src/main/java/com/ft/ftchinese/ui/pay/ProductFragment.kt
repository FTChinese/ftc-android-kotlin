package com.ft.ftchinese.ui.pay

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.Observer

import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.subsPlans
import kotlinx.android.synthetic.main.fragment_product.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ProductFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: ProductViewModel
    private var product: PaywallProduct? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        product = arguments?.getParcelable(ARG_PRODUCT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        product_heading_tv.text = product?.heading
        product_benefits_tv.text = product?.description

        if (product?.smallPrint.isNullOrBlank()) {
            product_small_print.visibility = View.GONE
        } else {
            product_small_print.visibility = View.VISIBLE
        }

        yearly_price_btn.text = product?.yearPrice
        yearly_price_btn.setOnClickListener {
            val tier = product?.tier ?: return@setOnClickListener
            viewModel.select(subsPlans.of(tier, Cycle.YEAR))
            it.isEnabled = false
        }

        if (product?.monthPrice.isNullOrBlank()) {
            monthly_price_btn.visibility = View.GONE
        } else {
            monthly_price_btn.text = product?.monthPrice

            monthly_price_btn.setOnClickListener {
                val tier = product?.tier ?: return@setOnClickListener
                viewModel.select(subsPlans.of(tier, Cycle.MONTH))
                it.isEnabled = false
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activity?.run {
            ViewModelProviders.of(this).get(ProductViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        // Waiting for signal from host activity to enable/disable buttons.
        viewModel.inputEnabled.observe(this, Observer<Boolean> {
            yearly_price_btn.isEnabled = it
            monthly_price_btn.isEnabled = it
        })
    }

    // Buttons are disabled upon click to prevent loading
    // new activity multiple times.
    // When the fragment is resumed, enable those buttons.
    override fun onResume() {
        super.onResume()
        yearly_price_btn.isEnabled = true
        monthly_price_btn.isEnabled = true
    }

    companion object {
        private const val ARG_PRODUCT = "arg_product"

        @JvmStatic
        fun newInstance(product: PaywallProduct) = ProductFragment().apply {
            arguments = bundleOf(ARG_PRODUCT to product)
        }
    }
}

