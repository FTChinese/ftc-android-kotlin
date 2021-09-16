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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
    }

    private fun setupViewModel() {

        checkoutViewModel = activity?.run {
            ViewModelProvider(this).get(CheckOutViewModel::class.java)
        } ?: throw Exception("Invalid activity")


//        checkoutViewModel.counterResult.observe(viewLifecycleOwner) { result: FetchResult<CheckoutCounter> ->
//            when (result) {
//                is FetchResult.LocalizedError -> toast(result.msgId)
//                is FetchResult.Error -> result.exception.message?.let { toast(it) }
//                is FetchResult.Success -> {
//
//                    val counter = result.data
//                    binding.cartItem = CartItem.from(requireContext(), counter.item)
//                }
//            }
//        }

        // Use the CheckoutItem to display ui.
        checkoutViewModel.counterLiveData.observe(viewLifecycleOwner) {
            binding.cartItem = CartItem.from(requireContext(), it.item)
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = CartItemFragment()
    }
}
