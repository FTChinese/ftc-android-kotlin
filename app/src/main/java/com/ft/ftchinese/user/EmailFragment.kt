package com.ft.ftchinese.user

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.fragment_email.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class EmailFragment : Fragment(),
        AnkoLogger {

    private var listener: OnCredentialsListener? = null
    private var job: Job? = null

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun enableInput(enable: Boolean) {
        email_input.isEnabled = enable
        next_btn.isEnabled = enable
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnCredentialsListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView finished")

        return inflater.inflate(R.layout.fragment_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Check whether email exists.
         */
        next_btn.setOnClickListener {
            val email = email_input.text.toString().trim()
            val isValid = isEmailValid(email)
            if (!isValid) {
                return@setOnClickListener
            }

            emailExists(email)
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

    /**
     * Ask API whether the mail exists.
     */
    private fun emailExists(email: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        showProgress(true)
        enableInput(false)
        val apiUrl = Uri.parse(NextApi.EMAIL_EXISTS)
                .buildUpon()
                .appendQueryParameter("k", "email")
                .appendQueryParameter("v", email)

        job = GlobalScope.launch(Dispatchers.Main) {

            try {
                val exists = withContext(Dispatchers.IO) {
                    val (resp, _) = Fetch()
                            .get(apiUrl.toString())
                            .responseApi()

                    resp.code() == 204
                }

                showProgress(false)

                if (!exists) {
                    toast("Unknown error encountered")
                    return@launch
                }

                listener?.onEmailFound(email)

            } catch (e: ClientError) {

                showProgress(false)

                if (e.statusCode == 404) {
                    // Show signup UI.
                    listener?.onEmailNotFound(email)

                    return@launch
                }

                enableInput(true)
                activity?.handleApiError(e)

                error(e)
            } catch (e: Exception) {

                showProgress(false)
                enableInput(true)

                activity?.handleException(e)

                error(e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun newInstance() = EmailFragment()
    }
}