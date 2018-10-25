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
import com.ft.ftchinese.models.PasswordUpdate
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.fragment_password.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class UpdatePasswordActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        mAccount = SessionManager.getInstance(this).loadUser()

        return PasswordFragment.newInstance()
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, UpdatePasswordActivity::class.java)

            context?.startActivity(intent)
        }
    }
}

class PasswordFragment : Fragment(), AnkoLogger {

    private var mAccount: Account? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    private var isInProgress: Boolean
        get() = !password_save_button.isEnabled
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean
        get() = new_password.isEnabled
        set(value) {
            old_password.isEnabled = value
            new_password.isEnabled = value
            confirm_password.isEnabled = value
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
            mAccount = mListener?.getUserSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        password_save_button.setOnClickListener {
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

        val uuid = mAccount?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = launch(UI) {
            val passwordUpdate = PasswordUpdate(oldPassword, newPassword)

            try {
                info("Start updating password")

                val statusCode = passwordUpdate.updateAsync(uuid).await()

                isInProgress = false

                if (statusCode == 204) {
                    toast(R.string.success_saved)
                } else {
                    toast("API response status: $statusCode")
                }

            } catch (e: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true


            } catch (e: Exception) {
                isInProgress = false
                isInputAllowed = true

                handleException(e)
            }
        }
    }

    private fun handleErrorResponse(resp: ErrorResponse) {
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
                handleApiError(resp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    companion object {
        fun newInstance() = PasswordFragment()
    }
}