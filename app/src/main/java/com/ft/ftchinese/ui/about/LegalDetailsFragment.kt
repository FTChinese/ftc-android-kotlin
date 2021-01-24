package com.ft.ftchinese.ui.about

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentLegalDetailsBinding
import com.ft.ftchinese.model.legal.legalDocs
import com.ft.ftchinese.ui.base.ScopedFragment
import io.noties.markwon.Markwon
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LegalDetailsFragment : ScopedFragment(), AnkoLogger {

    private lateinit var binding: FragmentLegalDetailsBinding
    private lateinit var markwon: Markwon
    private val args: LegalDetailsFragmentArgs by navArgs()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        markwon = Markwon.create(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_legal_details, container, false)
        // Inflate the layout for this fragment
        binding.showBtn = false
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val index = args.index
        val doc = legalDocs[index]
        view.findViewById<TextView>(R.id.heading_tv).text = doc.title
        val spanned = markwon.toMarkdown(doc.content)
        markwon.setParsedMarkdown(binding.contentTv, spanned)
    }

    companion object {
        @JvmStatic
        fun newInstance() = LegalDetailsFragment()
    }
}
