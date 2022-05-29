package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSignUpBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.mobile.MobileViewModel
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

/**
 * Popup when user signup required.
 * This dialog might appears in multiple locations:
 * AuthActivity -> EmailExistsFragment -> SignUpFragment
 * AuthActivity -> MobileFragment -> SignInFragment -> SignUpFragment
 * LinkFtcActivity -> SignUpFragment
 */
class SignUpFragment : ScopedBottomSheetDialogFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager

    private lateinit var emailViewModel: EmailViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var mobileViewModel: MobileViewModel

    private lateinit var binding: FragmentSignUpBinding

    private var usageKind: AuthKind? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        tokenManager = TokenManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(
            inflater.cloneInContext(
                ContextThemeWrapper(
                    requireContext(),
                    R.style.AppTheme,
                ),
            ),
            R.layout.fragment_sign_up,
            container,
            false,
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            usageKind = it.getParcelable(ARG_AUTH_KIND)
        }

        // Scoped to parent activity so that it could share data with email fragment.
        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        // To share dat with mobile fragment.
        mobileViewModel = activity?.run {
            ViewModelProvider(this)[MobileViewModel::class.java]
        } ?: throw Exception("Invalid activity")

        signUpViewModel = ViewModelProvider(this)[SignUpViewModel::class.java]

        connectionLiveData.observe(this) {
            signUpViewModel.isNetworkAvailable.value = it
        }
        context?.isConnected?.let {
            signUpViewModel.isNetworkAvailable.value = it
        }

        binding.viewModel = signUpViewModel
        binding.handler = this
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        emailViewModel.emailLiveData.observe(viewLifecycleOwner) {
            signUpViewModel.emailLiveData.value = it
        }

        signUpViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        signUpViewModel.accountResult.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.TextError -> toast(it.text)
                is FetchResult.Success -> onAccountLoaded(it.data)
            }
        }
    }

    private fun onAccountLoaded(account: Account) {
        when (usageKind) {
            AuthKind.EmailLogin,
            AuthKind.MobileLink -> {
                toast(R.string.prompt_signed_up)
            }
            AuthKind.WechatLink -> {
                toast(R.string.prompt_linked)
            }
        }

        sessionManager.saveAccount(account)
        context?.let {
            StatsTracker
                .getInstance(it)
                .setUserId(account.id)
        }
        activity?.setResult(Activity.RESULT_OK)
        activity?.finish()
    }

    private fun setupUI() {

        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }

        when (usageKind) {
            AuthKind.EmailLogin -> {
                binding.title = getString(R.string.title_sign_up)
                binding.guide = getString(R.string.instruct_sign_up)
                binding.emailInput.isEnabled = false
                binding.passwordInput.requestFocus()
            }
            AuthKind.MobileLink -> {
                binding.title = "绑定新邮箱"
                binding.guide = "手机号${mobileViewModel.mobileLiveData.value}将与新创建的邮箱账号绑定，下次可以直接使用手机号登录"
                binding.emailInput.requestFocus()
            }
            AuthKind.WechatLink -> {
                binding.title = "新建账号"
                binding.guide = "当前微信将与新创建的邮箱账号绑定"
                binding.emailInput.isEnabled = false
                binding.passwordInput.requestFocus()
            }
        }

        childFragmentManager.commit {
            replace(R.id.fuck_huawei_policy, ConsentPrivacyFragment.newInstance())
        }
    }

    fun onSubmit(view: View) {

        if (!binding.privacyConsent.isChecked) {
            toast("您需要同意用户协议和隐私政策")
            return
        }

        when (usageKind) {
            AuthKind.EmailLogin -> {
                signUpViewModel.emailSignUp(tokenManager.getToken())
            }
            AuthKind.WechatLink -> sessionManager
                .loadAccount()
                ?.unionId
                ?.let {
                    signUpViewModel
                        .wxSignUp(
                            deviceToken = tokenManager.getToken(),
                            unionId = it,
                        )
                }
        }
    }

    companion object {
        private const val ARG_AUTH_KIND = "arg_auth_kind"
        @JvmStatic
        fun newInstance(k: AuthKind) = SignUpFragment().apply {
            arguments = bundleOf(
                ARG_AUTH_KIND to k
            )
        }

        @JvmStatic
        fun forEmailLogin() = newInstance(AuthKind.EmailLogin)

        @JvmStatic
        fun forWechatLink() = newInstance(AuthKind.WechatLink)
    }
}
