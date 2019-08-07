package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.Credentials
import com.ft.ftchinese.model.TokenManager
import kotlinx.android.synthetic.main.fragment_sign_in.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignInFragment : ScopedFragment(),
        AnkoLogger {

    private var email: String? = null
    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: LoginViewModel

    private fun enableInput(enable: Boolean) {
        password_input.isEnabled = enable
        sign_in_btn.isEnabled = enable
    }

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

        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        instruct_sign_in_tv.text = getString(R.string.instruct_sign_in, email)

        password_input.requestFocus()
        sign_in_btn.isEnabled = false

        forgot_password_link.setOnClickListener {
            val e = email ?: return@setOnClickListener
            ForgotPasswordActivity.start(context, e)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        password_input.afterTextChanged {
            viewModel.passwordDataChanged(password_input.text.toString().trim())
        }

        viewModel.loginFormState.observe(this, Observer {
            info("login form state: $it")
            val loginState = it ?: return@Observer

            sign_in_btn.isEnabled = loginState.isPasswordValid

            if (loginState.error != null) {
                password_input.error = getString(loginState.error)
                password_input.requestFocus()
            }
        })


        sign_in_btn.setOnClickListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val email = email ?: return@setOnClickListener

            // TODO: move somewhere else.
            if (password_input.text.toString().trim().isEmpty()) {
                password_input.error = getString(R.string.error_invalid_password)
                return@setOnClickListener
            }

            enableInput(false)
            viewModel.showProgress(true)

            viewModel.login(Credentials(
                    email = email,
                    password = password_input.text.toString().trim(),
                    deviceToken = tokenManager.getToken()
            ))
        }

        viewModel.accountResult.observe(this, Observer {
            if (it.error != null || it.exception != null) {
                enableInput(true)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        enableInput(true)
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
