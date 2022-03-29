package com.ft.ftchinese.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.ft.ftchinese.ui.base.ConnectionLiveData
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class ScopedBottomSheetDialogFragment : BottomSheetDialogFragment(), CoroutineScope by MainScope(){

    protected lateinit var connectionLiveData: ConnectionLiveData

    override fun onAttach(context: Context) {
        super.onAttach(context)

        connectionLiveData = ConnectionLiveData(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.setOnShowListener {
            val dialog = it as BottomSheetDialog

            val parentLayout = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            parentLayout?.let { v ->
                val behavior = BottomSheetBehavior.from(v)
                setupFullHeight(v)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return bottomSheetDialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
