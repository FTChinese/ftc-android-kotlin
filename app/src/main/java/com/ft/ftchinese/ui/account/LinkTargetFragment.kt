package com.ft.ftchinese.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Membership
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.util.json
import kotlinx.android.synthetic.main.fragment_link_target.*
import org.jetbrains.anko.AnkoLogger

/**
 * account-to-be-merged.
 */
class LinkTargetFragment : Fragment(), AnkoLogger {

    private var membership: Membership? = null
    private var heading: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val m = arguments?.getString(ARG_MEMBERSHIP) ?: return

        membership = json.parse<Membership>(m)
        heading = arguments?.getString(ARG_HEADING)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_link_target, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tv_link_heading.text = heading
        tv_link_member_tier.text = when (membership?.tier) {
            Tier.STANDARD -> getString(R.string.tier_standard)
            Tier.PREMIUM -> getString(R.string.tier_premium)
            else -> getString(R.string.tier_free)
        }
        tv_member_expire_date.text = formatLocalDate(membership?.expireDate) ?: ""

    }

    companion object {
        private const val ARG_MEMBERSHIP = "arg_membership"
        private const val ARG_HEADING = "arg_heading"
        fun newInstance(m: Membership, heading: String? = null) = LinkTargetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MEMBERSHIP, json.toJsonString(m))
                putString(ARG_HEADING, heading)
            }
        }
    }
}
