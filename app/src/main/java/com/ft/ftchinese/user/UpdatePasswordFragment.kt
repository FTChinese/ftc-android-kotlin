package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.Passwords
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_update_password.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdatePasswordFragment : ScopedFragment(), AnkoLogger {

    private var listener: OnUpdateAccountListener? = null
    private var sessionManager: SessionManager? = null

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun allowInput(value: Boolean) {
        old_password_input.isEnabled = value
        new_password_input.isEnabled = value
        confirm_password_input.isEnabled = value
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

        return inflater.inflate(R.layout.fragment_update_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        save_btn.setOnClickListener {
            info("Saving password...")

            val passwords = validate() ?: return@setOnClickListener

            save(passwords)
        }
    }

    private fun validate(): Passwords? {
        old_password_input.error = null
        new_password_input.error = null
        confirm_password_input.error = null

        val oldPassword = old_password_input.text.toString().trim()
        val newPassword = new_password_input.text.toString().trim()
        val confirmPassword = confirm_password_input.text.toString().trim()

        if (oldPassword.isBlank()) {
            old_password_input.error = getString(R.string.error_invalid_password)
            old_password_input.requestFocus()
            return null
        }

        var msgId = Validator.ensurePassword(newPassword)
        if (msgId != null) {
            new_password_input.error = getString(msgId)
            new_password_input.requestFocus()
            return null
        }


        msgId = Validator.ensurePassword(confirmPassword)
        if (msgId != null) {
            confirm_password_input.error = getString(msgId)
            confirm_password_input.requestFocus()
            return null
        }

        if (newPassword != confirmPassword) {
            confirm_password_input.error = getString(R.string.error_mismatched_confirm_password)
            confirm_password_input.requestFocus()
            return null
        }

        return Passwords(
                oldPassword = oldPassword,
                newPassword = newPassword
        )
    }

    private fun save(passwords: Passwords) {

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        val uuid = sessionManager?.loadAccount()?.id ?: return

        showProgress(true)
        allowInput(false)

        launch {

            try {
                info("Start updating password")

                val done = withContext(Dispatchers.IO) {
                    FtcUser(uuid).updatePassword(passwords)
                }

                showProgress(false)

                info("Change password result: $done")

                toast(R.string.prompt_saved)

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                when (e.statusCode) {
                    // 422 could be password_invalid
                    404 -> {
                        toast(R.string.api_account_not_found)
                    }
                    // Wrong old password
                    403 -> {
                        toast(R.string.error_incorrect_old_password)
                    }
                    else -> {
                        activity?.handleApiError(e)
                    }
                }

            } catch (e: Exception) {
                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    companion object {
        fun newInstance() = UpdatePasswordFragment()
    }
}