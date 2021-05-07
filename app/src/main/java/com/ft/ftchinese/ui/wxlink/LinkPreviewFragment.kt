package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.login.SignInViewModel
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.wxapi.WxOAuthViewModel
import org.jetbrains.anko.AnkoLogger
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
class LinkPreviewFragment : ScopedBottomSheetDialogFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentLinkPreviewBinding

    // Used to retrieve login account
    private lateinit var loginViewModel: SignInViewModel
    // Perform link and refresh account
    private lateinit var linkViewModel: LinkViewModel
    // Used to retrieve wechat account after authorization.
    // TODO: add WxOAuthViewModel
    private lateinit var wxAuthViewModel: WxOAuthViewModel

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

        loginViewModel = activity?.run {
            ViewModelProvider(this)
                .get(SignInViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        wxAuthViewModel = activity?.run {
            ViewModelProvider(this)
                .get(WxOAuthViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        // Link view model does not rely on hosting activity.
        linkViewModel = ViewModelProvider(this)
            .get(LinkViewModel::class.java)

        binding.viewModel = linkViewModel
        binding.lifecycleOwner = this
        binding.handler = this

        setupViewModel()
    }

    private fun setupViewModel() {
        linkViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        // After email account authenticated.
        // Current account is wechat.
        loginViewModel.accountResult.observe(viewLifecycleOwner) {
            if (it !is FetchResult.Success) {
                toast("Email account not found in view model")
                return@observe
            }

            sessionManager.loadAccount()?.let { current ->
                initUI(LinkParams(
                    ftc = it.data,
                    wx = current,
                    loginMethod = current.loginMethod ?: LoginMethod.WECHAT
                ))
            }
        }

        // After wechat account authorized.
        // Current account is ftc.
        wxAuthViewModel.accountResult.observe(viewLifecycleOwner) {
            if (it !is FetchResult.Success) {
                toast("Email account not found in view model")
                return@observe
            }

            sessionManager.loadAccount()?.let { current ->
                initUI(LinkParams(
                    ftc = current,
                    wx = it.data,
                    loginMethod = current.loginMethod ?: LoginMethod.EMAIL
                ))
            }
        }

        // TODO: email link wechat.
        linkViewModel.accountLinked.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_linked)
                    sessionManager.saveAccount(it.data)
                    /**
                     * Pass data back to [LinkFtcActivity].
                     * If this activity is started from WxEntryActivity,
                     * it is meaningless to pass data back.
                     * Unwrapping chain:
                     * [AccountActivity] <- [LinkFtcActivity] <- current activity.
                     */
                    activity?.setResult(Activity.RESULT_OK)
                    activity?.finish()
                }
            }
        }
    }

    private fun initUI(params: LinkParams) {

        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }

        childFragmentManager.commit {
            replace(R.id.frag_ftc_account, LinkTargetFragment.newInstance(
                m = params.ftc.membership,
                heading = "${getString(R.string.label_ftc_account)}\n${params.ftc.email}"
            )
            )

            replace(R.id.frag_wx_account, LinkTargetFragment.newInstance(
                m = params.wx.membership,
                heading = "${getString(R.string.label_wx_account)}\n${params.wx.wechat.nickname}"
            )
            )
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

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun startForResult(activity: Activity?, account: Account) {
            val intent = Intent(activity, LinkPreviewFragment::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account)
            }

            activity?.startActivityForResult(intent, RequestCode.LINK)
        }
    }
}
