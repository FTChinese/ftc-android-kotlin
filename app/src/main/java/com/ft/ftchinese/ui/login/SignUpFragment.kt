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
import com.ft.ftchinese.databinding.FragmentSignUpBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.base.ScopedBottomSheetDialogFragment
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * Popup when user signup required.
 * This dialog might appears in 3 locations:
 * * Email signup
 * * User is trying to login with mobile for the first time, an email account is required.
 * * A wx user is trying to link to a new account.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignUpFragment : ScopedBottomSheetDialogFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var binding: FragmentSignUpBinding

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

        binding.passwordInput.requestFocus()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emailViewModel = activity?.run {
            ViewModelProvider(this)
                .get(EmailExistsViewModel::class.java)
        } ?: throw Exception("Invalid activity")

        signUpViewModel = activity?.run {
            ViewModelProvider(this)
                .get(SignUpViewModel::class.java)
        } ?: throw Exception("Invalid activity")

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

        signUpViewModel.isFormEnabled.observe(viewLifecycleOwner) {
            binding.isFormEnabled = it
        }
    }

    private fun setupUI() {
        binding.toolbar.bottomSheetToolbar.onClick {
            dismiss()
        }
    }

    fun onSubmit(view: View) {

        val account = sessionManager.loadAccount()

        // If account exists, this should be wechat signup.
        if (account == null) {
            signUpViewModel.signUp(tokenManager.getToken())
        } else {
            if (account.isWxOnly) {
                account.unionId?.let {
                    signUpViewModel.wxSignUp(tokenManager.getToken(), it)
                }
            }
        }
    }
}
