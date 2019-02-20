package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.PasswordReset
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_forgot_password.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

internal class ForgotPasswordFragment : Fragment(), AnkoLogger {

    private var job: Job? = null
    private var listener: OnCredentialsListener? = null
    private var email: String? = null

    private fun showProgress(value: Boolean) {
        listener?.onProgress(value)

        // If in progress, hide the message telling what to do next.
        if (value) {
            letter_sent_tv?.visibility = View.GONE
        }
    }

    private fun allowInput(value: Boolean) {
        send_email_btn?.isEnabled = value
        email_input?.isEnabled = value
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnCredentialsListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        email = arguments?.getString(ARG_EMAIL)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email_input.text = Editable.Factory.getInstance().newEditable(email)

        send_email_btn.setOnClickListener {

            val email = email_input.text.toString()

            val isValid = isEmailValid(email)

            if (!isValid) {
                return@setOnClickListener
            }

            sendLetter(email)
        }
    }

    /**
     * Validate email. Returns true if it is valid; otherwise false.
     */
    private fun isEmailValid(email: String): Boolean {
        email_input.error = null

        val msgId = Validator.ensureEmail(email)

        if (msgId != null) {
            email_input.error = getString(msgId)
            email_input.requestFocus()

            return false
        }

        return true
    }

    private fun sendLetter(emailStr: String) {

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {
            val passwordReset = PasswordReset(emailStr)

            try {
                val done = withContext(Dispatchers.IO) {
                    passwordReset.send()
                }

                if (done) {
                    toast(R.string.success_letter_sent)

                    showProgress(false)
                } else {
                    toast("Sending email failed. Pleas try again")
                    showProgress(false)
                    allowInput(true)
                }

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                info("API error response: ${e.message}")
                handleClientError(e)

            } catch (e: Exception) {

                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    /**
     * Handle restful api error response
     */
    private fun handleClientError(resp: ClientError) {
        when (resp.statusCode) {
            // Email is not found
            404 -> {
                toast(R.string.api_email_not_found)
            }
            else -> {
                activity?.handleApiError(resp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {

        fun newInstance(email: String) = ForgotPasswordFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }
    }
}