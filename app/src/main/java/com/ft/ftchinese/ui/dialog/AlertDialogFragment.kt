package com.ft.ftchinese.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.ft.ftchinese.R

class AlertDialogFragment : DialogFragment() {

    private var positiveButtonListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, _ ->
        dialog.dismiss()
    }

    private var negativeButtonListener: DialogInterface.OnClickListener = DialogInterface.OnClickListener { dialog, _ ->
        dialog.dismiss()
    }

    fun onPositiveButtonClicked(listener: DialogInterface.OnClickListener): AlertDialogFragment {
        this.positiveButtonListener = listener
        return this
    }

    fun onNegativeButtonClicked(listener: DialogInterface.OnClickListener): AlertDialogFragment {
        this.negativeButtonListener = listener
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = arguments?.getParcelable(ARG_DIALOG_PARAMS) ?: DialogArgs(
            positiveButton = R.string.btn_ok,
            negativeButton = R.string.btn_cancel,
            message = "",
        )

        return activity?.let {
            val builder = AlertDialog.Builder(it)
                .setMessage(args.message)
                .setPositiveButton(args.positiveButton, positiveButtonListener)

            if (args.title != null) {
                builder.setTitle(args.title)
            }

            if (args.negativeButton != null) {
                builder.setNegativeButton(args.negativeButton, negativeButtonListener)
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val ARG_DIALOG_PARAMS = "arg_dialog_params"

        @JvmStatic
        fun newInstance(
            params: DialogArgs? = null
        ) = AlertDialogFragment().apply {
            arguments = bundleOf(
                ARG_DIALOG_PARAMS to params
            )
        }

        @JvmStatic
        fun newMsgInstance(msg: String) = newInstance(
            params = DialogArgs(
                message = msg,
                positiveButton = R.string.btn_ok
            )
        )

    }
}
