package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.PasswordReset
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_forgot_password.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class ForgotPasswordActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return ForgotPasswordFragment.newInstance()
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, ForgotPasswordActivity::class.java)

            context?.startActivity(intent)
        }
    }
}

internal class ForgotPasswordFragment : Fragment(), AnkoLogger {

    private var job: Job? = null
    private var listener: OnFragmentInteractionListener? = null
    private var isInProgress: Boolean
        get() = !send_button.isEnabled
        set(value) {
            listener?.onProgress(value)

            // If in progress, hide the message telling what to do next.
            if (value) {
                letter_sent_feedback.visibility = View.GONE
            }
        }
    private var isInputAllowed: Boolean
        get() = send_button.isEnabled
        set(value) {
            send_button.isEnabled = value
            email.isEnabled = value
        }

    private fun failureState() {
        isInProgress = false
        isInputAllowed = true
        letter_sent_feedback.visibility = View.GONE
    }

    private fun successState() {
        isInProgress = true
        isInputAllowed = false
        letter_sent_feedback.visibility = View.VISIBLE
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        send_button.setOnClickListener {
            attemptSendLetter()
        }
    }

    private fun attemptSendLetter() {
        email.error = null

        val emailStr = email.text.toString()

        var cancel = false

        if (emailStr.isBlank()) {
            email.error = getString(R.string.error_field_required)
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            cancel = true
        }

        if (cancel) {
            email.requestFocus()

            return
        }

        sendLetter(emailStr)
    }

    private fun sendLetter(emailStr: String) {
        isInProgress = true
        isInputAllowed = false

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        job = launch(UI) {
            val passwordReset = PasswordReset(emailStr)

            try {
                val statusCode = passwordReset.sendAsync().await()

                if (statusCode == 204) {
                    toast(R.string.success_letter_sent)

                    isInProgress = false
                } else {
                    toast("API response status: $statusCode")
                    isInProgress = false
                    isInputAllowed = true
                }

            } catch (e: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true

                info("API error response: ${e.message}")
                handleErrorResponse(e)

            } catch (e: Exception) {
                isInProgress = false
                isInputAllowed = true

                handleException(e)
            }
        }
    }

    /**
     * Handle restful api error response
     */
    private fun handleErrorResponse(resp: ErrorResponse) {
        when (resp.statusCode) {
            // 422 could be email_invalid
            // Email is not found
            404 -> {
                toast(R.string.api_email_not_found)
            }
            else -> {
                handleApiError(resp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun newInstance(): ForgotPasswordFragment {
            return ForgotPasswordFragment()
        }
    }
}
