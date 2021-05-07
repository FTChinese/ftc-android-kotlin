package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSignInBinding
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.base.ScopedBottomSheetDialogFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.sdk27.coroutines.onClick

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
class SignInFragment() : ScopedBottomSheetDialogFragment(),
        AnkoLogger {

    private lateinit var tokenManager: TokenManager
    private lateinit var loginViewModel: SignInViewModel
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var binding: FragmentSignInBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        tokenManager = TokenManager.getInstance(context)
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

        binding.passwordInput.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel = activity?.run {
            ViewModelProvider(this)
                .get(SignInViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailExistsViewModel::class.java)
        } ?: throw Exception("Invalid activity")

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

        // TODO: get mobile number from mobile view model.

        loginViewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.inProgress = it
        }

        loginViewModel.isFormEnabled.observe(viewLifecycleOwner) {
            binding.isFormEnabled = it
        }
    }

    private fun setupUI() {
        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }
    }

    fun onSubmit(view: View) {
        loginViewModel.login(tokenManager.getToken())
    }

    fun onClickForgotPassword(view: View) {
        ForgotPasswordActivity.start(context, loginViewModel.emailLiveData.value ?: "")
    }

    fun onClickCreateAccount(view: View) {
        SignUpFragment().show(childFragmentManager, "SignUpFragment")
    }
}
