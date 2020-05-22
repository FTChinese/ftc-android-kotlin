package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentSignInBinding
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.store.TokenManager
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.LoginViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignInFragment : ScopedFragment(),
        AnkoLogger {

    private var email: String? = null
    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: FragmentSignInBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)

        tokenManager = TokenManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        email = arguments?.getString(ARG_EMAIL)

        if (email.isNullOrBlank()) {
            toast("Email not set!")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_in, container, false)

        binding.email = email
        binding.passwordInput.requestFocus()
        binding.signInBtn.isEnabled = false

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.forgotPasswordLink.setOnClickListener {
            val e = email ?: return@setOnClickListener
            ForgotPasswordActivity.start(context, e)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        binding.passwordInput.afterTextChanged {
            viewModel.passwordDataChanged(binding.passwordInput.text.toString().trim())
        }

        viewModel.loginFormState.observe(viewLifecycleOwner, Observer {
            info("login form state: $it")
            val loginState = it ?: return@Observer

            binding.signInBtn.isEnabled = loginState.isPasswordValid

            if (loginState.error != null) {
                binding.passwordInput.error = getString(loginState.error)
                binding.passwordInput.requestFocus()
            }
        })


        binding.signInBtn.setOnClickListener {
            if (context?.isConnected != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val email = email ?: return@setOnClickListener

            // TODO: move somewhere else.
            if (binding.passwordInput.text.toString().trim().isEmpty()) {
                binding.passwordInput.error = getString(R.string.error_invalid_password)
                return@setOnClickListener
            }

            binding.enableInput = false
            viewModel.inProgress.value = true

            viewModel.login(Credentials(
                    email = email,
                    password = binding.passwordInput.text.toString().trim(),
                    deviceToken = tokenManager.getToken()
            ))
        }

        // Observer account in fragment only to enable/disable button.
        // Host activity will handle the account data.
        viewModel.accountResult.observe(viewLifecycleOwner, Observer {
            binding.enableInput = it !is Result.Success
        })
    }

    override fun onResume() {
        super.onResume()
        binding.enableInput = true
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        @JvmStatic
        fun newInstance(email: String) = SignInFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }
    }
}
