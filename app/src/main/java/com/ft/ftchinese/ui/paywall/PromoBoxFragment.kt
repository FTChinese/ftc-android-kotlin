package com.ft.ftchinese.ui.paywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentPromoBoxBinding
import com.ft.ftchinese.ui.product.ProductViewModel


/**
 * A simple [Fragment] subclass.
 * Use the [PromoBoxFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PromoBoxFragment : Fragment() {

    private lateinit var productViewModel: ProductViewModel
    private lateinit var binding: FragmentPromoBoxBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_promo_box, container, false)

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        productViewModel = activity?.run {
            ViewModelProvider(this).get(ProductViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        productViewModel.promoReceived.observe(viewLifecycleOwner, {
            binding.promo = it
        })
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PromoBoxFragment.
         */
        @JvmStatic
        fun newInstance() = PromoBoxFragment()
    }
}
