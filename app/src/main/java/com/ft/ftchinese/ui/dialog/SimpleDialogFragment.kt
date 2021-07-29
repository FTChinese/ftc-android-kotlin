package com.ft.ftchinese.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R

class SimpleDialogFragment : DialogFragment() {

    private lateinit var viewModel: DialogViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                .get(DialogViewModel::class.java)
        } ?: throw IllegalStateException("Invalid activity")
    }

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
                    viewModel.positiveButtonClicked.value = true
                }

            if (params.negative != null) {
                builder.setNegativeButton(params.negative) { dialog, _ ->
                    dialog.dismiss()
                    viewModel.negativeButtonClicked.value = true
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
