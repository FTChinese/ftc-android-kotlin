package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_update_email.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateEmailFragment : ScopedFragment(), AnkoLogger {

    private var listener: OnUpdateAccountListener? = null
    private var sessionManager: SessionManager? = null

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun allowInput(value: Boolean) {
        email_input.isEnabled = value
        save_btn.isEnabled = value
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)

        if (context is OnUpdateAccountListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        value_email_tv.text = sessionManager?.loadAccount()?.email

        save_btn.setOnClickListener {
            val email = email_input.text.toString().trim()
            val isValid = isEmailValid(email)

            if (!isValid) {
                return@setOnClickListener
            }

            save(email)
        }
    }

    private fun isEmailValid(email: String): Boolean {
        email_input.error = null

        val msgId = Validator.ensureEmail(email)
        if (msgId != null) {
            email_input.error = getString(msgId)
            email_input.requestFocus()
            return false
        }

        val currentEmail = value_email_tv.text.toString()

        if (currentEmail == email) {
            email_input.error = getString(R.string.error_email_unchanged)
            email_input.requestFocus()

            return false
        }

        return true
    }

    private fun save(emailStr: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        // If user id is not found, we could not perform updating.
        val uuid = sessionManager?.loadAccount()?.id ?: return

        showProgress(true)
        allowInput(false)

        launch {

            try {
                info("Start updating email")

                val done = withContext(Dispatchers.IO) {
                    FtcUser(uuid).updateEmail(emailStr)
                }

                showProgress(false)

                if (done) {

                    toast(R.string.prompt_saved)

                    listener?.onUpdateAccount()
                } else {

                }
            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                activity?.handleApiError(e)

            } catch (e: Exception) {
                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    companion object {
        fun newInstance() = UpdateEmailFragment()
    }
}