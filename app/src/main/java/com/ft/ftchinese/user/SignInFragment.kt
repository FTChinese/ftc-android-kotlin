package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Credentials
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class SignInFragment : Fragment(), AnkoLogger {
    private var job: Job? = null
    private var listener: OnCredentialsListener? = null
//    private var sessionManager: SessionManager? = null
    private var email: String? = null

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun enableInput(enable: Boolean) {
        password_input.isEnabled = enable
        sign_in_btn.isEnabled = enable
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnCredentialsListener) {
            listener = context
        }
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

        showProgress(true)
        enableInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val userId = withContext(Dispatchers.IO) {
                    Credentials(email, password).login()
                }

                showProgress(false)

                if (userId == null) {
                    toast(R.string.error_not_loaded)
                    enableInput(true)
                    return@launch
                }

                info("Login success: $userId")

                listener?.onLoggedIn(userId)

            } catch (e: ClientError) {
                showProgress(false)
                enableInput(true)

                if (e.statusCode == 404) {
                    toast(R.string.api_wrong_credentials)
                    return@launch
                }

                activity?.handleApiError(e)
            } catch (e: Exception) {
                showProgress(false)
                enableInput(true)
                activity?.handleException(e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {

        fun newInstance(email: String) = SignInFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }
    }
}