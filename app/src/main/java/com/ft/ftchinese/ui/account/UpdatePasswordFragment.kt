package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.Passwords
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.ui.Validator
import kotlinx.android.synthetic.main.fragment_update_password.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast
import java.lang.Exception

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdatePasswordFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel

    private fun enableInput(value: Boolean) {
        old_password_input.isEnabled = value
        new_password_input.isEnabled = value
        confirm_password_input.isEnabled = value
        save_btn.isEnabled = value
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_password, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        setUp()
    }

    private fun setUp() {

        save_btn.setOnClickListener {
            val passwords = validate() ?: return@setOnClickListener

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            updateViewModel.showProgress(true)
            enableInput(false)

            updateViewModel.updatePassword(
                    userId = userId,
                    passwords = passwords
            )
        }

        // Re-enable input in case errors
        updateViewModel.updateResult.observe(this, Observer {
            if (it.error != null || it.exception != null) {
                enableInput(true)
            }
        })
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

    companion object {

        @JvmStatic
        fun newInstance() = UpdatePasswordFragment()
    }
}
