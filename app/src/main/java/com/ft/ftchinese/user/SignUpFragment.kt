package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Credentials
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_sign_up.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

class SignUpFragment : Fragment(), AnkoLogger {
    private var job: Job? = null
    private var listener: OnCredentialsListener? = null
    private var sessionManager: SessionManager? = null
    private var email: String? = null

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun enableInput(enable: Boolean) {
        password_input.isEnabled = enable
        sign_up_btn.isEnabled = enable
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnCredentialsListener) {
            listener = context
        }

        if (context != null) {
            sessionManager = SessionManager.getInstance(context)
        }
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

        sign_up_btn.setOnClickListener {
            val password = password_input.text.toString().trim()
            val isValid = isPasswordValid(password)
            if (!isValid) {
                return@setOnClickListener
            }

            val e = email ?: return@setOnClickListener
            signUp(e, password)
        }
    }

    private fun isPasswordValid(pw: String): Boolean {
        password_input.error = null

        val msgId = Validator.ensurePassword(pw)
        if (msgId != null) {
            password_input.error = getString(msgId)
            password_input.requestFocus()

            return false
        }

        return true
    }

    private fun signUp(email: String, password: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        showProgress(true)
        enableInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val account = withContext(Dispatchers.IO) {
                    Credentials(email, password).signUp()
                }

                showProgress(false)

                if (account == null) {
                    toast(R.string.error_not_loaded)
                    enableInput(true)
                    return@launch
                }

                sessionManager?.saveAccount(account)

                activity?.setResult(Activity.RESULT_OK)

                activity?.finish()
                
            } catch (e: ClientError) {
                showProgress(false)
                enableInput(true)

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
        fun newInstance(email: String) = SignUpFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }
    }
}