package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUpdateAddressBinding
import com.ft.ftchinese.model.reader.Address

/**
 * A simple [Fragment] subclass.
 * Use the [UpdateAddressFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class UpdateAddressFragment : Fragment() {

    private lateinit var binding: FragmentUpdateAddressBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_update_address, container, false)
        binding.address = Address()

        // Inflate the layout for this fragment
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *UpdateAddressFragment.
         */
        @JvmStatic
        fun newInstance() =
            UpdateAddressFragment()
    }
}
