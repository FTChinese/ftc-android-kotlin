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

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignInFragment : ScopedBottomSheetDialogFragment(),
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

    private fun setupViewModel() {
        emailViewModel.emailLiveData.observe(viewLifecycleOwner) {
            loginViewModel.emailLiveData.value = it
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

    companion object {
        private const val ARG_EMAIL = "arg_email"

        @Deprecated("Use no arg version")
        @JvmStatic
        fun newInstance(email: String) = SignInFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }

        @JvmStatic
        fun newInstance() = SignInFragment()
    }
}
