package com.ft.ftchinese.user

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Membership
import com.ft.ftchinese.util.formatLocalDate
import com.ft.ftchinese.base.getMemberTypeText
import com.ft.ftchinese.util.json
import kotlinx.android.synthetic.main.fragment_membership.*
import org.jetbrains.anko.AnkoLogger

/**
 * Used by [AccountsMergeActivity] to show details of each
 * account-to-be-merged.
 */
class MembershipFragment : Fragment(), AnkoLogger {

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

        return inflater.inflate(R.layout.fragment_membership, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        heading_tv.text = heading

        val rows = buildRows(membership)

        val viewManager = LinearLayoutManager(context)
        val viewAdapter = RowAdapter(rows)

        member_rv.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }

    private fun buildRows(member: Membership?): Array<TableRow> {
        if (member == null) {
            return arrayOf()
        }

        val row1 = TableRow(
                header = getString(R.string.label_member_tier),
                data = activity?.getMemberTypeText(member) ?: ""
        )

        val row2 = TableRow(
                header = getString(R.string.label_member_duration),
                data = formatLocalDate(member.expireDate) ?: ""
        )

        return arrayOf(row1, row2)
    }

    companion object {
        private const val ARG_MEMBERSHIP = "arg_membership"
        private const val ARG_HEADING = "arg_heading"
        fun newInstance(m: Membership, heading: String? = null) = MembershipFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MEMBERSHIP, json.toJsonString(m))
                putString(ARG_HEADING, heading)
            }
        }
    }
}