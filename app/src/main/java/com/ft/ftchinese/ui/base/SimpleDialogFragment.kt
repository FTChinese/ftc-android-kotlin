package com.ft.ftchinese.ui.base

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R

class SimpleDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = arguments?.getString(ARG_DIALOG_MESSAGE) ?: ""

        return activity?.let {
            AlertDialog.Builder(it)
                .setMessage(message)
                .setPositiveButton(R.string.action_ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val ARG_DIALOG_MESSAGE = "arg_dialog_message"

        @JvmStatic
        fun newInstance(m: String) = SimpleDialogFragment().apply {
            arguments = bundleOf(
                ARG_DIALOG_MESSAGE to m,
            )
        }
    }
}
