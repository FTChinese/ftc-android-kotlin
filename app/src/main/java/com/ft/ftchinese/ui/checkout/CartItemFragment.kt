package com.ft.ftchinese.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentCartItemBinding
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.formatter.getCurrencySymbol

/**
 * Used to show the an overview of the item user purchased.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CartItemFragment : ScopedFragment() {

    private lateinit var binding: FragmentCartItemBinding
    private lateinit var cartViewModel: CartItemViewModel

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

        binding.price = CartItem(
            productName = "",
        )
        binding.hasDiscount = false
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        cartViewModel = activity?.run {
            ViewModelProvider(this).get(CartItemViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        cartViewModel.priceInCart.observe(viewLifecycleOwner) {
            binding.price = CartItem.from(requireContext(), it)
        }

        cartViewModel.discountsFound.observe(viewLifecycleOwner) {
            if (it.hasDiscount) {
                binding.hasDiscount = true

                val options = it.items.map { discount ->
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
            }
        }

        binding.discountSpinner.setOnItemClickListener { _, _, pos, _ ->
            cartViewModel.discountChanged.value = pos
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = CartItemFragment()
    }
}
