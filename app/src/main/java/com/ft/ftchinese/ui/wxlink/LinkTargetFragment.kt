package com.ft.ftchinese.ui.wxlink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentLinkTargetBinding
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.fetch.formatLocalDate

/**
 * account-to-be-merged.
 */
@Deprecated("")
class LinkTargetFragment : Fragment() {

    private var membership: Membership? = null
    private var heading: String? = null
    private lateinit var binding: FragmentLinkTargetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        membership = arguments?.getParcelable(ARG_MEMBERSHIP)
        heading = arguments?.getString(ARG_HEADING)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_link_target, container, false)
        buildUI(heading, membership)

        return binding.root
    }

    private fun buildUI(heading: String?, membership: Membership?) {
        binding.member = UIMemberStatus(
                heading = heading,
                tier = getString(membership?.tier?.stringRes ?: R.string.tier_free),
                expireDate = formatLocalDate(membership?.expireDate) ?: ""
        )
    }

    companion object {
        private const val ARG_MEMBERSHIP = "arg_membership"
        private const val ARG_HEADING = "arg_heading"
        fun newInstance(m: Membership, heading: String? = null) = LinkTargetFragment().apply {

            arguments = bundleOf(
                    ARG_MEMBERSHIP to m,
                    ARG_HEADING to heading
            )
        }
    }
}
