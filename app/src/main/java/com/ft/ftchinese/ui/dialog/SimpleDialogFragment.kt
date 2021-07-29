package com.ft.ftchinese.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R

class SimpleDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val params = arguments?.getParcelable(ARG_SIMPLE_DIALOG) ?: DialogParams(
            positive = getString(R.string.action_ok),
            negative = getString(R.string.action_cancel),
            message = ""
        )

        return activity?.let {
            val builder = AlertDialog.Builder(it)
                .setMessage(params.message)
                .setPositiveButton(params.positive) { dialog, _ ->
                    dialog.dismiss()
                }

            if (params.negative != null) {
                builder.setNegativeButton(params.negative) { dialog, _ ->
                    dialog.dismiss()
                }
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val ARG_SIMPLE_DIALOG = "arg_simple_dialog"

        @JvmStatic
        fun newInstance(params: DialogParams) = SimpleDialogFragment().apply {
            arguments = bundleOf(
                ARG_SIMPLE_DIALOG to params
            )
        }
    }
}
