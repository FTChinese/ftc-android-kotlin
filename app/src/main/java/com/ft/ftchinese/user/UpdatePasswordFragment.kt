package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Passwords
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_update_password.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


class UpdatePasswordFragment : Fragment(), AnkoLogger {

    private var job: Job? = null
    private var mListener: OnAccountInteractionListener? = null

    private var mSession: SessionManager? = null

    private var isInProgress: Boolean
        get() = !save_button.isEnabled
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean
        get() = new_password.isEnabled
        set(value) {
            old_password?.isEnabled = value
            new_password?.isEnabled = value
            confirm_password?.isEnabled = value
            save_button?.isEnabled = value
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnAccountInteractionListener) {
            mListener = context
        }

        if (context != null) {
            mSession = SessionManager.getInstance(context)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        save_button.setOnClickListener {
            info("Saving password...")
            attemptSave()
        }
    }

    private fun attemptSave() {
        old_password.error = null
        new_password.error = null
        confirm_password.error = null

        val oldPassword = old_password.text.toString().trim()
        val newPassword = new_password.text.toString().trim()
        val confirmPassword = confirm_password.text.toString().trim()

        var cancel = false
        var focusView: View? = null

        if (oldPassword.isBlank()) {
            old_password.error = getString(R.string.error_invalid_password)
            focusView = old_password
            cancel = true
        }

        if (newPassword.isBlank()) {
            new_password.error = getString(R.string.error_invalid_password)
            focusView = new_password
            cancel = true
        }

        if (confirmPassword.isBlank()) {
            confirm_password.error = getString(R.string.error_invalid_password)
            focusView = confirm_password
            cancel = true
        }

        if (newPassword != confirmPassword) {
            confirm_password.error = getString(R.string.error_mismatched_confirm_password)
            focusView = confirm_password
            cancel = true
        }

        if (cancel) {
            focusView?.requestFocus()

            return
        }

        save(oldPassword, newPassword)
    }

    private fun save(oldPassword: String, newPassword: String) {

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        val uuid = mSession?.loadAccount()?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = GlobalScope.launch(Dispatchers.Main) {
            val passwordUpdate = Passwords(oldPassword, newPassword)

            try {
                info("Start updating password")

                val done = withContext(Dispatchers.IO) {
                    passwordUpdate.send(uuid)
                }

                isInProgress = false

                if (done) {
                    toast(R.string.success_saved)
                } else {

                }

            } catch (e: ClientError) {
                isInProgress = false
                isInputAllowed = true

                handleClientError(e)

            } catch (e: Exception) {
                isInProgress = false
                isInputAllowed = true

                activity?.handleException(e)
            }
        }
    }

    private fun handleClientError(resp: ClientError) {
        when (resp.statusCode) {
            // 422 could be password_invalid
            404 -> {
                toast(R.string.api_account_not_found)
            }
            // Wrong old password
            403 -> {
                toast(R.string.error_incorrect_old_password)
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
        fun newInstance() = UpdatePasswordFragment()
    }
}