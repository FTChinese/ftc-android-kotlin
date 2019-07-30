package com.ft.ftchinese.ui.account

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProviders

import com.ft.ftchinese.R
import com.ft.ftchinese.model.UnlinkAnchor
import kotlinx.android.synthetic.main.fragment_unlink_anchor.*

private const val ARG_IS_STRIPE = "arg_is_stripe"

class UnlinkAnchorFragment : Fragment() {

    private lateinit var viewModel: LinkViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_unlink_anchor, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(LinkViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        anchor_ftc_btn.setOnClickListener {
            viewModel.selectAnchor(UnlinkAnchor.FTC)
        }

        anchor_wx_btn.setOnClickListener {
            viewModel.selectAnchor(UnlinkAnchor.WECHAT)
        }

        if (arguments?.getBoolean(ARG_IS_STRIPE) == true) {
            anchor_ftc_btn.isChecked  = true
            anchor_wx_btn.isEnabled = false
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
