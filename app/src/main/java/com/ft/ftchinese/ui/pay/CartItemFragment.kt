package com.ft.ftchinese.ui.pay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentCartItemBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.getTierCycleText
import com.ft.ftchinese.model.subscription.PaymentIntent

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CartItemFragment : ScopedFragment() {

//    private var ftcPlan: Plan? = null
    private var paymentIntent: PaymentIntent? = null
    lateinit var binding: FragmentCartItemBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cart_item, container, false)
        return binding.root
//        return inflater.inflate(R.layout.fragment_cart_item, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paymentIntent = arguments?.getParcelable(EXTRA_PAYMENT_INTENT)
        binding.tvPrice.text = getString(
                R.string.formatter_price,
                paymentIntent?.currencySymbol(),
                paymentIntent?.amount)

        binding.tvTitle.text = activity?.getTierCycleText(paymentIntent?.plan?.tier, paymentIntent?.plan?.cycle)
    }

    companion object {

        @JvmStatic
        fun newInstance(pi: PaymentIntent?) = CartItemFragment().apply {
            arguments = bundleOf(EXTRA_PAYMENT_INTENT to pi)
        }
    }
}
