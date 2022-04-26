package com.ft.ftchinese.ui.article

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentAiAudioBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.webpage.BaseJsEventListener
import com.ft.ftchinese.ui.webpage.JsInterface
import com.ft.ftchinese.ui.webpage.WVClient
import com.ft.ftchinese.ui.webpage.configWebView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AiAudioFragment : BottomSheetDialogFragment() {

    private lateinit var articleViewModel: ArticleViewModel
    private lateinit var binding: FragmentAiAudioBinding
    private lateinit var session: SessionManager

    override fun onAttach(context: Context) {
        super.onAttach(context)
        session = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ai_audio, container, false)

        binding.handler = this
        return binding.root
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        articleViewModel = activity?.run {
            ViewModelProvider(
                this,
            )[ArticleViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        configWebView(
            webView = binding.audioWebView,
            jsInterface = JsInterface(BaseJsEventListener(requireContext())),
            client = WVClient(
                context = requireContext(),
            )
        )

        articleViewModel.aiAudioTeaser
            ?.htmlUrl(session.loadAccount())
            ?.let { url ->
                binding.audioWebView.loadUrl(url)
            }
    }

    fun onCloseBottomSheet(view: View) {
        dismiss()
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         */
        @JvmStatic
        fun newInstance() = AiAudioFragment()
    }
}
