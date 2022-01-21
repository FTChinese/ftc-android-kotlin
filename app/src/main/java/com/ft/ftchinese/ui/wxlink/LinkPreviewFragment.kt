package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentLinkPreviewBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.base.isConnected
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

/**
 * Show details of account to be bound, show a button to let
 * user to confirm the performance, or just deny accounts merging.
 * It has 2 usages:
 * 1. Wx-only user tries to link to an existing email account
 * 2. Email user wants to link to wechat.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkPreviewFragment(
    private val params: WxEmailLink
) : ScopedBottomSheetDialogFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentLinkPreviewBinding

    // Perform link and refresh account
    private lateinit var linkViewModel: LinkViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
            inflater.cloneInContext(
                ContextThemeWrapper(
                    requireContext(),
                    R.style.AppTheme,
                ),
            ),
            R.layout.fragment_link_preview,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Link view model does not rely on hosting activity.
        linkViewModel = ViewModelProvider(this)
            .get(LinkViewModel::class.java)

        connectionLiveData.observe(this) {
            linkViewModel.isNetworkAvailable.value = it
        }
        activity?.isConnected?.let {
            linkViewModel.isNetworkAvailable.value
        }

        binding.viewModel = linkViewModel
        binding.lifecycleOwner = this
        binding.handler = this

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        linkViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        linkViewModel.accountLinked.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> {
                    AlertDialog.Builder(requireContext())
                        .setMessage(it.msgId)
                        .setPositiveButton(R.string.action_ok) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()
                        .show()
                }
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_linked)
                    sessionManager.saveAccount(it.data)
                    activity?.setResult(Activity.RESULT_OK)
                    activity?.finish()
                }
            }
        }
    }

    private fun initUI() {

        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }

        childFragmentManager.commit {
            replace(R.id.frag_ftc_account, LinkTargetFragment.newInstance(
                m = params.ftc.membership,
                heading = "${getString(R.string.label_ftc_account)}\n${params.ftc.email}"
            ))

            replace(R.id.frag_wx_account, LinkTargetFragment.newInstance(
                m = params.wx.membership,
                heading = "${getString(R.string.label_wx_account)}\n${params.wx.wechat.nickname}"
            ))
        }

        val result = params.link(requireContext())

        if (result.denied != null) {
            binding.resultTv.text = result.denied
            return
        }

        linkViewModel.linkableLiveData.value = result.linked
    }

    fun onClickLink(view: View) {
        linkViewModel.link()
    }
}
