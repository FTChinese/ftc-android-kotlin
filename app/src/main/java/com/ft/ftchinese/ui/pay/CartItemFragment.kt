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
import com.ft.ftchinese.databinding.FragmentCartItemBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.getTierCycleText
import com.ft.ftchinese.model.subscription.PaymentIntent
import com.ft.ftchinese.viewmodel.CheckOutViewModel
import com.ft.ftchinese.viewmodel.Result

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CartItemFragment : ScopedFragment() {

    private var paymentIntent: PaymentIntent? = null
    private lateinit var binding: FragmentCartItemBinding
    private lateinit var checkOutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentIntent = arguments?.getParcelable(EXTRA_PAYMENT_INTENT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_cart_item, container, false)
        paymentIntent?.let {
            binding.product = buildProduct(it)
        }

        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        checkOutViewModel = activity?.run {
            ViewModelProvider(this).get(CheckOutViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        checkOutViewModel.stripePlanResult.observe(viewLifecycleOwner, Observer {
            if (it !is Result.Success) {
                return@Observer
            }

            paymentIntent = paymentIntent?.withStripePlan(it.data)?.also { paymentIntent ->
                binding.product = buildProduct(paymentIntent)
            }
        })
    }

    private fun formatPrice(currency: String?, amount: Double?): String {
        if (currency == null || amount == null) {
            return "..."
        }

        if (amount == 0.0) {
            return "..."
        }

        return getString(
                R.string.formatter_price,
                currency,
                amount)
    }

    private fun buildProduct(pi: PaymentIntent): UIProductInCart {
        return UIProductInCart(
                price = formatPrice(
                        pi.currencySymbol(),
                        pi.amount
                ),
                name = activity?.getTierCycleText(pi.plan.tier, pi.plan.cycle) ?: ""
        )
    }

    companion object {

        @JvmStatic
        fun newInstance(pi: PaymentIntent?) = CartItemFragment().apply {
            arguments = bundleOf(EXTRA_PAYMENT_INTENT to pi)
        }
    }
}
