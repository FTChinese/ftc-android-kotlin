package com.ft.ftchinese.ui.main

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentLegalDetailsBinding
import com.ft.ftchinese.model.legal.legalDocs
import com.ft.ftchinese.store.ServiceAcceptance
import io.noties.markwon.Markwon
import org.jetbrains.anko.AnkoLogger

/**
 * https://developer.android.com/guide/topics/ui/dialogs#FullscreenDialog
 */
class AcceptServiceDialogFragment : DialogFragment(), AnkoLogger {

    private lateinit var acceptance: ServiceAcceptance
    private lateinit var binding: FragmentLegalDetailsBinding
    private lateinit var markwon: Markwon

    override fun onAttach(context: Context) {
        super.onAttach(context)
        acceptance = ServiceAcceptance.getInstance(context)
        markwon = Markwon.create(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_legal_details, container, false)
        binding.showBtn = true
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val doc = legalDocs[0]
        view.findViewById<TextView>(R.id.heading_tv).text = doc.title
        val spanned = markwon.toMarkdown(doc.content)
        markwon.setParsedMarkdown(binding.contentTv, spanned)

        binding.declineBtn.setOnClickListener {
            dismiss()
            activity?.finish()
        }

        binding.acceptBtn.setOnClickListener {
            dismiss()
            acceptance.accepted()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Prevent dismiss when touching outside the dialog window.
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(false)
        dialog.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
                activity?.finish()
            }
            false
        }

        dialog.setCanceledOnTouchOutside(false)
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }
}
