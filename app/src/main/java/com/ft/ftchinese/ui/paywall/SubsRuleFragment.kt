package com.ft.ftchinese.ui.paywall

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.R
import io.noties.markwon.Markwon

class SubsRuleFragment : Fragment() {

    private lateinit var markwon: Markwon

    override fun onAttach(context: Context) {
        super.onAttach(context)
        markwon = Markwon.create(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_subs_rule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tv: TextView = view.findViewById(R.id.subs_rule_guide)

        markwon.setParsedMarkdown(tv, markwon.toMarkdown(paywallGuide))
    }

    companion object {
        @JvmStatic
        fun newInstance() = SubsRuleFragment()
    }
}
