package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.model.Result
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.reader.Credentials
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.TokenManager
import com.ft.ftchinese.viewmodel.LoginViewModel
import kotlinx.android.synthetic.main.fragment_sign_up.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignUpFragment : ScopedFragment(),
        AnkoLogger {

    private var email: String? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager
    private lateinit var viewModel: LoginViewModel

    private fun enableInput(enable: Boolean) {
        password_input.isEnabled = enable
        sign_up_btn.isEnabled = enable
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        tokenManager = TokenManager.getInstance(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        email = arguments?.getString(ARG_EMAIL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        instruct_sign_up_tv.text = getString(R.string.instruct_sign_up, email)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProvider(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        viewModel.loginFormState.observe(viewLifecycleOwner, Observer {
            val signUpState = it ?: return@Observer

            sign_up_btn.isEnabled = signUpState.isPasswordValid

            if (signUpState.error != null) {
                password_input.error = getString(signUpState.error)
                password_input.requestFocus()
            }
        })


        password_input.requestFocus()
        password_input.afterTextChanged {
            viewModel.passwordDataChanged(password_input.text.toString().trim())
        }

        // TODO: handle wechat sign-up.
        sign_up_btn.setOnClickListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val e = email ?: return@setOnClickListener

            if (password_input.text.toString().trim().isEmpty()) {
                password_input.error = getString(R.string.error_invalid_password)
                return@setOnClickListener
            }

            enableInput(false)
            viewModel.inProgress.value = true

            viewModel.signUp(
                c = Credentials(
                        email = e,
                        password = password_input.text.toString().trim(),
                        deviceToken = tokenManager.getToken()
                )
            )
        }

        viewModel.accountResult.observe(viewLifecycleOwner, Observer {
//            if (it.error != null || it.exception != null) {
//                enableInput(true)
//            }
            enableInput(it !is Result.Success)
        })
    }

    companion object {

        private const val ARG_EMAIL = "arg_email"

        @JvmStatic
        fun newInstance(email: String) = SignUpFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }
    }
}
