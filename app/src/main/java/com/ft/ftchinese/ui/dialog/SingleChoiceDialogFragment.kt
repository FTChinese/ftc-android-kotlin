package com.ft.ftchinese.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment

class SingleChoiceDialogFragment : DialogFragment() {

    internal lateinit var listener: Listener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as Listener
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() +
                " must implement NoticeDialogListener"))
        }
    }

    interface Listener {
        fun onSelectSingleChoiceItem(id: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val args = arguments?.getParcelable(ARG_SINGLE_CHOICE) ?: SingleChoiceArgs(
            title = "Please choose",
            choices = arrayOf()
        )

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(args.title)
                .setItems(args.choices) { dialog, which ->
                    dialog.dismiss()
                    listener.onSelectSingleChoiceItem(which)
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    companion object {
        private const val ARG_SINGLE_CHOICE = "arg_dialog_args"

        @JvmStatic
        fun newInstance(params: SingleChoiceArgs) = SingleChoiceDialogFragment().apply {
            arguments = bundleOf(
                ARG_SINGLE_CHOICE to params
            )
        }
    }
}
