package com.ft.ftchinese.ui.main

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentAcceptServiceDialogBinding
import com.ft.ftchinese.store.ServiceAcceptance
import com.ft.ftchinese.ui.webabout.WebpageFragment
import com.ft.ftchinese.ui.webabout.legalPages
import org.jetbrains.anko.AnkoLogger

/**
 * https://developer.android.com/guide/topics/ui/dialogs#FullscreenDialog
 */
class AcceptServiceDialogFragment : DialogFragment(), AnkoLogger {

    private lateinit var acceptance: ServiceAcceptance
    private lateinit var binding: FragmentAcceptServiceDialogBinding
//    private lateinit var markwon: Markwon

    override fun onAttach(context: Context) {
        super.onAttach(context)
        acceptance = ServiceAcceptance.getInstance(context)
//        markwon = Markwon.create(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_accept_service_dialog, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val page = legalPages[0]

        childFragmentManager.commit {
            replace(R.id.terms_holder, WebpageFragment.newInstance(page.url))
        }
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
