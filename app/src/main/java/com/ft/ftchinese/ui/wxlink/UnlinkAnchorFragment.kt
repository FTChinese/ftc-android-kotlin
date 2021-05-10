package com.ft.ftchinese.ui.wxlink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentUnlinkAnchorBinding
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.reader.UnlinkAnchor
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
            ViewModelProvider(this)
                .get(UnlinkViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        binding.btnAnchorFtc.setOnClickListener {
            viewModel.selectAnchor(UnlinkAnchor.FTC)
        }

        binding.btnAnchorWx.setOnClickListener {
            viewModel.selectAnchor(UnlinkAnchor.WECHAT)
        }

        binding.isStripe = AccountCache.get()?.let {
            it.membership.payMethod == PayMethod.STRIPE
        } ?: false
    }

    companion object {
        @JvmStatic
        fun newInstance() = UnlinkAnchorFragment()
    }
}
