package com.ft.ftchinese.ui.wxlink

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUnlinkAnchorBinding
import com.ft.ftchinese.model.reader.UnlinkAnchor

private const val ARG_IS_STRIPE = "arg_is_stripe"

class UnlinkAnchorFragment : Fragment() {

    private lateinit var viewModel: UnlinkViewModel
    private lateinit var binding: FragmentUnlinkAnchorBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_unlink_anchor, container, false)
        binding.isStripe = arguments?.getBoolean(ARG_IS_STRIPE)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                    .get(UnlinkViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        binding.btnAnchorFtc.setOnClickListener {
            viewModel.selectAnchor(UnlinkAnchor.FTC)
        }

        binding.btnAnchorWx.setOnClickListener {
            viewModel.selectAnchor(UnlinkAnchor.WECHAT)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(isStripe: Boolean) = UnlinkAnchorFragment().apply {
            arguments = bundleOf(
                   ARG_IS_STRIPE to isStripe
            )
        }
    }
}
