package com.ft.ftchinese.ui.wxlink

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUnlinkAnchorBinding
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.enums.UnlinkAnchor
import com.ft.ftchinese.store.AccountCache

class UnlinkAnchorFragment : Fragment() {

    private lateinit var viewModel: UnlinkViewModel
    private lateinit var binding: FragmentUnlinkAnchorBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_unlink_anchor,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)[UnlinkViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        binding.btnAnchorFtc.setOnClickListener {
            Log.i(TAG, "Clicked FTC button")
            viewModel.selectAnchor(UnlinkAnchor.FTC)
        }

        binding.btnAnchorWx.setOnClickListener {
            Log.i(TAG, "Clicked Wx button")
            viewModel.selectAnchor(UnlinkAnchor.WECHAT)
        }

        val isFtcSideOnly = AccountCache.get()?.let {
            arrayOf(PayMethod.STRIPE, PayMethod.APPLE, PayMethod.B2B).contains(it.membership.payMethod)
        } ?: false
        binding.isFtcSideOnly = isFtcSideOnly
        if (isFtcSideOnly) {
            viewModel.selectAnchor(UnlinkAnchor.FTC)
        }
    }

    companion object {
        private const val TAG = "UnlinkAnchorFragment"

        @JvmStatic
        fun newInstance() = UnlinkAnchorFragment()
    }
}
