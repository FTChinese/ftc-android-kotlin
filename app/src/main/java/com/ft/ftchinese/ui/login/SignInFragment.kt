package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSignInBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.enums.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.auth.password.PasswordActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.mobile.MobileViewModel
import com.ft.ftchinese.ui.wxlink.LinkPreviewFragment
import com.ft.ftchinese.ui.wxlink.WxEmailLinkAccounts
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

/**
 * Authenticate an existing email account.
 * It might appears in 3 location:
 * 1. Email login: the loaded account should be saved locally;
 * 2. User is trying to login with mobile for the first time,
 * and is asked to link to an existing account. The mobile,
 * together with email and password, will be sent to server.
 * 3. A wx user is trying to link to an existing account.
 * The loaded account will be used for link preview.
 */
class SignInFragment : ScopedBottomSheetDialogFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var loginViewModel: SignInViewModel
    private lateinit var emailViewModel: EmailViewModel
    private lateinit var mobileViewModel: MobileViewModel
    private lateinit var binding: FragmentSignInBinding

    private var kind: AuthKind? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        tokenManager = TokenManager.getInstance(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        // NOTE: dialog does not inherit app's them.
        binding = DataBindingUtil.inflate(
            inflater.cloneInContext(
                ContextThemeWrapper(
                    requireContext(),
                    R.style.AppTheme,
                ),
            ),
            R.layout.fragment_sign_in,
            container,
            false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            kind = it.getParcelable(ARG_AUTH_KIND)
        }

        Log.i(TAG, "Usage kind $kind")

        // Used to retrieve email from hosting activity
        emailViewModel = activity?.run {
            ViewModelProvider(this)[EmailViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        // Used to retrieve mobile from hosting activity.
        mobileViewModel = activity?.run {
            ViewModelProvider(this)[MobileViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        // Scoped to parent activity so that when used
        // for wechat link, the preview fragment could
        // retrieve the loaded account.
        loginViewModel = ViewModelProvider(this)[SignInViewModel::class.java]

        connectionLiveData.observe(this) {
            loginViewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            loginViewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = loginViewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()
        setupUI()
    }

    /**
     * The account loaded after credentials verified is not
     * handled here since the account might be used for
     * various purposes.
     * It is handled by the [AuthActivity] for email or mobile
     * login, or [com.ft.ftchinese.ui.wxlink.LinkFtcActivity]
     * for link prview.
     */
    private fun setupViewModel() {
        // Get data from parent activity's input box.
        emailViewModel.emailLiveData.observe(viewLifecycleOwner) {
            loginViewModel.emailLiveData.value = it
        }

        loginViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        loginViewModel.accountResult.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> alertError(getString(it.msgId))
                is FetchResult.TextError -> toast(it.text)
                is FetchResult.Success -> onAccountLoaded(it.data)
            }
        }
    }

    private fun alertError(msg: String) {
        AlertDialogFragment
            .newMsgInstance(msg)
            .onPositiveButtonClicked { dialog, _ ->
                dialog.dismiss()
            }
            .show(childFragmentManager, "LoginError")
    }

    private fun onAccountLoaded(account: Account) {
        when (kind) {
            AuthKind.EmailLogin,
            AuthKind.MobileLink -> {
                toast(R.string.login_success)
                sessionManager.saveAccount(account)
                context?.let {
                    StatsTracker
                        .getInstance(it)
                        .setUserId(account.id)
                }
                activity?.setResult(Activity.RESULT_OK)
                activity?.finish()
            }
            AuthKind.WechatLink -> {
                Log.i(TAG, "Wechat is linking to an existing account $account")
                sessionManager.loadAccount()?.let { current ->
                    LinkPreviewFragment(
                        WxEmailLinkAccounts(
                            ftc = account, // Retrieved account
                            wx = current, // Currently logged-in account
                            loginMethod = current.loginMethod ?: LoginMethod.WECHAT,
                        )
                    ).show(
                        childFragmentManager,
                        "PreviewWechatLinkEmail",
                    )
                    // You cannot call dismiss here since all child dialog will be dismissed.
                }
            }
        }
    }

    private fun setupUI() {
        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }

        when(kind) {
            AuthKind.EmailLogin -> {
                binding.title = getString(R.string.title_login)
                binding.guide = getString(R.string.instruct_sign_in)
                binding.emailInput.isEnabled = false
                binding.passwordInput.requestFocus()
                binding.showCreateAccount = false
            }
            AuthKind.MobileLink -> {
                binding.title = "绑定已有邮箱账号"
                binding.guide = "绑定邮箱后下次可以直接使用手机号${mobileViewModel.mobileLiveData.value}登录该邮箱账号"
                binding.emailInput.requestFocus()
                binding.showCreateAccount = true
            }
            AuthKind.WechatLink -> {
                binding.title = "验证密码"
                binding.guide = "验证邮箱账号密码后绑定微信"
                binding.emailInput.isEnabled = false
                binding.passwordInput.requestFocus()
                binding.showCreateAccount = false
            }
        }
    }

    fun onSubmit(view: View) {
        when (kind) {
            AuthKind.EmailLogin,
            AuthKind.WechatLink -> loginViewModel.emailAuth(tokenManager.getToken())
            AuthKind.MobileLink -> {
                val mobile = mobileViewModel.mobileLiveData.value
                if (mobile.isNullOrBlank()) {
                    toast("Missing mobile number!")
                    return
                }
                loginViewModel.mobileLinkEmail(
                    mobile = mobile,
                    deviceToken = tokenManager.getToken()
                )
            }
        }
    }

    fun onClickForgotPassword(view: View) {
        PasswordActivity.start(context, loginViewModel.emailLiveData.value ?: "")
    }

    fun onClickCreateAccount(view: View) {
        kind?.let {
            SignUpFragment
                .newInstance(it)
                .show(childFragmentManager, "SignUpFragment")
        }
    }

    companion object {
        private const val TAG = "SignInFragment"
        private const val ARG_AUTH_KIND = "arg_auth_kind"

        @JvmStatic
        fun newInstance(k: AuthKind) = SignInFragment().apply {
            arguments = bundleOf(
                ARG_AUTH_KIND to k
            )
        }

        @JvmStatic
        fun forEmailLogin() = newInstance(AuthKind.EmailLogin)

        @JvmStatic
        fun forMobileLink() = newInstance(AuthKind.MobileLink)

        @JvmStatic
        fun forWechatLink() = newInstance(AuthKind.WechatLink)
    }
}
