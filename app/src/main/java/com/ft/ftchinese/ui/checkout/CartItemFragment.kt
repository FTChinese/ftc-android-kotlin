package com.ft.ftchinese.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentCartItemBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.formatter.getCurrencySymbol
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.support.v4.toast

/**
 * Used to show the an overview of the item user purchased.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CartItemFragment : ScopedFragment() {

    private lateinit var binding: FragmentCartItemBinding
    private lateinit var checkoutViewModel: CheckOutViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_cart_item,
            container,
            false
        )

        binding.cartItem = CartItem(
            productName = "",
        )
        binding.hasDiscount = false
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        checkoutViewModel = activity?.run {
            ViewModelProvider(this).get(CheckOutViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        checkoutViewModel.counterResult.observe(viewLifecycleOwner) { result: Result<CheckoutCounter> ->
            when (result) {
                is Result.LocalizedError -> toast(result.msgId)
                is Result.Error -> result.exception.message?.let { toast(it) }
                is Result.Success -> {

                    val counter = result.data
                    binding.cartItem = CartItem.from(requireContext(), counter.checkoutItem)

                    initSpinner(counter.discountOptions)
                }
            }
        }

        checkoutViewModel.checkoutItem.observe(viewLifecycleOwner) {
            binding.cartItem = CartItem.from(requireContext(), it)
        }

        binding.discountSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                checkoutViewModel.changeDiscount(pos)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun initSpinner(opts: DiscountOptions) {
        if (!opts.hasDiscount) {
            return
        }

        binding.hasDiscount = true

        val options = opts.discounts.map { discount ->
            "限时促销 -${context?.getString(R.string.formatter_price, getCurrencySymbol(discount.currency), discount.priceOff)}"
        }

        ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            options
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.discountSpinner.adapter = adapter
        }
        binding.discountSpinner.setSelection(opts.spinnerIndex)
    }

    companion object {

        @JvmStatic
        fun newInstance() = CartItemFragment()
    }
}
