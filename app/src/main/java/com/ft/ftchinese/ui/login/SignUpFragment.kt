package com.ft.ftchinese.ui.login

import android.app.Activity
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
import com.ft.ftchinese.databinding.FragmentSignUpBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.ui.dialog.ScopedBottomSheetDialogFragment
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.mobile.MobileViewModel
import io.noties.markwon.Markwon
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.support.v4.toast

/**
 * Popup when user signup required.
 * This dialog might appears in 3 locations:
 * * Email signup
 * * User is trying to login with mobile for the first time, an email account is required.
 * * A wx user is trying to link to a new account.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignUpFragment(
    private val kind: AuthKind
) : ScopedBottomSheetDialogFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager

    private lateinit var emailViewModel: EmailViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var mobileViewModel: MobileViewModel

    private lateinit var binding: FragmentSignUpBinding
    private lateinit var markwon: Markwon

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        tokenManager = TokenManager.getInstance(context)
        markwon = Markwon.create(context)
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

        // Scoped to parent activity so that it could share data with email fragment.
        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        // To share dat with mobile fragment.
        mobileViewModel = activity?.run {
            ViewModelProvider(this)
                .get(MobileViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        signUpViewModel = ViewModelProvider(this)
            .get(SignUpViewModel::class.java)

        connectionLiveData.observe(this) {
            signUpViewModel.isNetworkAvailable.value = it
        }

        activity?.isNetworkConnected()?.let {
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

        mobileViewModel.mobileLiveData.observe(viewLifecycleOwner) {
            signUpViewModel.mobileLiveData.value = it
        }

        signUpViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        signUpViewModel.accountResult.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> onAccountLoaded(it.data)
            }
        }
    }

    private fun onAccountLoaded(account: Account) {
        when (kind) {
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

        when (kind) {
            AuthKind.EmailLogin -> {
                binding.title = getString(R.string.title_sign_up)
                binding.guide = getString(R.string.instruct_sign_up)
                binding.emailInput.isEnabled = false
                binding.passwordInput.requestFocus()
            }
            AuthKind.MobileLink -> {
                binding.title = "绑定新邮箱"
                binding.guide = "您的手机号将与新创建的邮箱账号绑定"
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

        when (kind) {
            AuthKind.EmailLogin -> signUpViewModel.emailSignUp(tokenManager.getToken())
            AuthKind.MobileLink -> signUpViewModel.mobileSignUp(tokenManager.getToken())
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
        @JvmStatic
        fun forEmailLogin() = SignUpFragment(AuthKind.EmailLogin)

        @JvmStatic
        fun forWechatLink() = SignUpFragment(AuthKind.WechatLink)
    }
}
