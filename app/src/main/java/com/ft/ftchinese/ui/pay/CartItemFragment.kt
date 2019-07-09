package com.ft.ftchinese.ui.pay

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.ft.ftchinese.R
import com.ft.ftchinese.base.getTierCycleText
import com.ft.ftchinese.model.order.PlanPayable
import kotlinx.android.synthetic.main.fragment_cart_item.*


class CartItemFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cart_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val plan = arguments?.getParcelable<PlanPayable>(EXTRA_PLAN_PAYABLE)

        tv_net_price.text = getString(R.string.formatter_price, plan?.currencySymbol(), plan?.payable)

        tv_product_overview.text = activity?.getTierCycleText(plan?.tier, plan?.cycle)
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CartItemFragment.
         */
        @JvmStatic
        fun newInstance(plan: PlanPayable?) =
                CartItemFragment().apply {
                    arguments = Bundle().apply {
                        putParcelable(EXTRA_PLAN_PAYABLE, plan)
                    }
                }
    }
}
