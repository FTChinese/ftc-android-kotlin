package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSignInBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.mobile.MobileViewModel
import com.ft.ftchinese.ui.wxlink.WxEmailLink
import com.ft.ftchinese.ui.wxlink.LinkPreviewFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
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
@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignInFragment(
    private val kind: AuthKind
) : ScopedBottomSheetDialogFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var loginViewModel: SignInViewModel
    private lateinit var emailViewModel: EmailViewModel
    private lateinit var mobileViewModel: MobileViewModel
    private lateinit var binding: FragmentSignInBinding

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

        // Used to retrieve email from hosting activity
        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        // Used to retrieve mobile from hosting activity.
        mobileViewModel = activity?.run {
            ViewModelProvider(this)
                .get(MobileViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        // Scoped to parent activity so that when used
        // for wechat link, the preview fragment could
        // retrieve the loaded account.
        loginViewModel = ViewModelProvider(this)
            .get(SignInViewModel::class.java)

        connectionLiveData.observe(this) {
            loginViewModel.isNetworkAvailable.value = it
        }
        activity?.isNetworkConnected()?.let {
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

        // Pass mobile number on.
        mobileViewModel.mobileLiveData.observe(viewLifecycleOwner) {
            loginViewModel.mobileLiveData.value = it
        }

        loginViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        loginViewModel.accountResult.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> handleErrMsgId(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> onAccountLoaded(it.data)
            }
        }
    }

    private fun handleErrMsgId(id: Int) {
        when (id) {
            R.string.mobile_link_taken -> {
                AlertDialog.Builder(requireContext())
                    .setMessage(id)
                    .setPositiveButton(R.string.action_done) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
            else  -> toast(id)
        }
    }

    private fun onAccountLoaded(account: Account) {
        when (kind) {
            AuthKind.EmailLogin,
            AuthKind.MobileLink -> {
                toast(R.string.prompt_logged_in)
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
                info("Wechat is linking to an existing account $account")
                sessionManager.loadAccount()?.let { current ->
                    LinkPreviewFragment(
                        WxEmailLink(
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
                binding.guide = "首次使用手机号码登录需要绑定邮箱账号，您可以验证已有邮箱账号或创建新账号。\n绑定邮箱后下次可以直接使用手机号码登录"
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
            AuthKind.MobileLink -> loginViewModel.mobileLinkEmail(tokenManager.getToken())
        }
    }

    fun onClickForgotPassword(view: View) {
        ForgotPasswordActivity.start(context, loginViewModel.emailLiveData.value ?: "")
    }

    fun onClickCreateAccount(view: View) {
        SignUpFragment(kind)
            .show(childFragmentManager, "SignUpFragment")
    }

    companion object {
        @JvmStatic
        fun forEmailLogin() = SignInFragment(AuthKind.EmailLogin)

        @JvmStatic
        fun forMobileLink() = SignInFragment(AuthKind.MobileLink)

        @JvmStatic
        fun forWechatLink() = SignInFragment(AuthKind.WechatLink)
    }
}
