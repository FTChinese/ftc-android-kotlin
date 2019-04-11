package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Credentials
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.TokenManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_sign_up.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast


class SignUpFragment : Fragment(), AnkoLogger {
    private var job: Job? = null
    private var listener: OnCredentialsListener? = null
    private var email: String? = null
    private var hostType: Int? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var tokenManager: TokenManager

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun enableInput(enable: Boolean) {
        password_input.isEnabled = enable
        sign_up_btn.isEnabled = enable
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        tokenManager = TokenManager.getInstance(context)

        if (context is OnCredentialsListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        email = arguments?.getString(ARG_EMAIL)
        hostType = arguments?.getInt(ARG_HOST_TYPE)
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

        /**
         * Used to determine which endpoint to access.
         * If unionId is not null, use /wx/signup,
         * else use /users/signup
         */
        val unionId = when (hostType) {
            HOST_BINDING_ACTIVITY -> sessionManager.loadWxSession()?.unionId
            else -> null
        }

        showProgress(true)
        enableInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val userId = withContext(Dispatchers.IO) {
                    Credentials(
                            email = email,
                            password = password,
                            deviceToken = tokenManager.getToken()
                    ).signUp(unionId)
                }

                showProgress(false)

                if (userId == null) {
                    toast(R.string.prompt_sign_up_failed)
                    enableInput(true)
                    return@launch
                }

                toast(R.string.prompt_signed_up)

                /**
                 * Tell hosting activity to fetch account.
                 */
                listener?.onSignedUp(userId)
                
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

        fun newInstance(email: String, host: Int) = SignUpFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
                putInt(ARG_HOST_TYPE, host)
            }
        }
    }
}