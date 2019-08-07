package com.ft.ftchinese.ui.pay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.getTierCycleText
import com.ft.ftchinese.model.order.Plan
import kotlinx.android.synthetic.main.fragment_cart_item.*

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CartItemFragment : ScopedFragment() {

    private var ftcPlan: Plan? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cart_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ftcPlan = arguments?.getParcelable(EXTRA_FTC_PLAN)

        initUI()
    }

    private fun initUI() {
        tv_net_price.text = getString(R.string.formatter_price, ftcPlan?.currencySymbol(), ftcPlan?.netPrice)

        tv_product_overview.text = activity?.getTierCycleText(ftcPlan?.tier, ftcPlan?.cycle)
    }

    companion object {

        @JvmStatic
        fun newInstance(plan: Plan?) =
                CartItemFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(EXTRA_FTC_PLAN, plan)
                    }
                }
    }
}
