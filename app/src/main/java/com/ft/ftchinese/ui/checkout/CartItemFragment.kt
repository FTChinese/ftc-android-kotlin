package com.ft.ftchinese.ui.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentCartItemBinding
import com.ft.ftchinese.ui.base.ScopedFragment

/**
 * Used to show the an overview of the item user purchased.
 */
class CartItemFragment : ScopedFragment() {

    private lateinit var binding: FragmentCartItemBinding
    private lateinit var cartViewModel: ShoppingCartViewModel

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
    }

    private fun setupViewModel() {

        cartViewModel = activity?.run {
            ViewModelProvider(this)[ShoppingCartViewModel::class.java]
        } ?: throw Exception("Invalid activity")


        cartViewModel.itemLiveData.observe(viewLifecycleOwner) {
            binding.cartItem = it
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = CartItemFragment()
    }
}
