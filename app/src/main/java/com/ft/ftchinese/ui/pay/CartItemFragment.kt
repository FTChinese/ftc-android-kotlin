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
import com.ft.ftchinese.viewmodel.CheckOutViewModel

const val ARG_CART_ITEM = "arg_cart_item"

/**
 * Used to show the an overview of the item user purchased.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class CartItemFragment : ScopedFragment() {

    private var cartItem: CartItem? = null
    private lateinit var binding: FragmentCartItemBinding
    private lateinit var checkOutViewModel: CheckOutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cartItem = arguments?.getParcelable(ARG_CART_ITEM)
    }

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

        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

//        checkOutViewModel = activity?.run {
//            ViewModelProvider(this).get(CheckOutViewModel::class.java)
//        } ?: throw Exception("Invalid Exception")
//
//        checkOutViewModel.stripePriceResult.observe(viewLifecycleOwner, Observer {
//            if (it !is Result.Success) {
//                return@Observer
//            }
//
//            paymentIntent = paymentIntent?.withStripePlan(it.data)?.also { paymentIntent ->
//                binding.product = buildProduct(paymentIntent)
//            }
//        })
//
        cartItem?.let {
            binding.product = it
        }
    }


    companion object {

        @JvmStatic
        fun newInstance(cartItem: CartItem) = CartItemFragment().apply {
            arguments = bundleOf(ARG_CART_ITEM to cartItem)
        }
    }
}
