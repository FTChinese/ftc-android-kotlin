package com.ft.ftchinese.ui.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.models.Credentials
import com.ft.ftchinese.models.TokenManager
import com.ft.ftchinese.user.ForgotPasswordActivity
import com.ft.ftchinese.util.ClientError
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

        email = arguments?.getString(com.ft.ftchinese.user.ARG_EMAIL)

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
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        sign_in_btn.setOnClickListener {
            val password = password_input.text.toString().trim()
            val isValid = isPasswordValid(password)
            if (!isValid) {
                return@setOnClickListener
            }

            val email = email ?: return@setOnClickListener

            login(email, password)
        }

        forgot_password_link.setOnClickListener {
            val e = email ?: return@setOnClickListener
            ForgotPasswordActivity.start(context, e)
        }
    }

    private fun isPasswordValid(pw: String): Boolean {
        password_input.error = null

        // To be backward compatible, only checks password is not empty.
        if (pw.isBlank()) {
            password_input.error = getString(R.string.error_field_required)
            password_input.requestFocus()

            return false
        }

        return true
    }

    private fun login(email: String, password: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        viewModel.showProgress(true)
        enableInput(false)

        launch {
            try {
                val userId = withContext(Dispatchers.IO) {
                    Credentials(
                            email = email,
                            password = password,
                            deviceToken = tokenManager.getToken()
                    ).login()
                }

                viewModel.showProgress(false)

                if (userId == null) {
                    toast(R.string.prompt_login_failed)
                    enableInput(true)
                    return@launch
                }

                info("Login success: $userId")

                viewModel.setUserId(userId)

            } catch (e: ClientError) {
                viewModel.showProgress(false)
                enableInput(true)

                if (e.statusCode == 404) {
                    toast(R.string.api_wrong_credentials)
                    return@launch
                }

                activity?.handleApiError(e)
            } catch (e: Exception) {
                viewModel.showProgress(false)
                enableInput(true)
                activity?.handleException(e)
            }
        }
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