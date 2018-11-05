package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.EmailUpdate
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_email.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class UpdateEmailActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        mAccount = SessionManager.getInstance(this).loadUser()

        return EmailFragment.newInstance()
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, UpdateEmailActivity::class.java)

            context?.startActivity(intent)
        }
    }
}

internal class EmailFragment : Fragment(), AnkoLogger {

    private var mAccount: Account? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    /**
     * Set progress indicator.
     * By default, the progress bar is not visible and data input and save button is enabled.
     * In case of any error, progress bar should go away and data input and save button should be re-enabled.
     * In case of successfully uploaded data, progress bar should go away but the data input and save button should be disabled to prevent mAccount re-submitting the same data.
     */
    private var isInProgress: Boolean
        get() = !save_button.isEnabled
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean
        get() = email.isEnabled
        set(value) {
            email.isEnabled = value
            save_button.isEnabled = value
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

        return inflater.inflate(R.layout.fragment_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        save_button.setOnClickListener {
            attemptSave()
        }

        // Check if user has email set.
        val email = mAccount?.email ?: return

        current_email.text = email
    }

    private fun attemptSave() {
        email.error = null
        val emailStr = email.text.toString()

        var cancel = false

        // If email is empty
        if (emailStr.isBlank()) {
            email.error = getString(R.string.error_field_required)
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            // If email is invalid
            email.error = getString(R.string.error_field_required)
            cancel = true
        } else if (emailStr == mAccount?.email) {
            // If new email equals the current one
            email.error = getString(R.string.error_email_unchanged)
            cancel = true
        }

        if (cancel) {
            email.requestFocus()
            return
        }

        save(emailStr)
    }

    private fun save(emailStr: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        // If user id is not found, we could not perform updating.
        val uuid = mAccount?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = GlobalScope.launch(Dispatchers.Main) {
            val emailUpdate = EmailUpdate(emailStr)

            try {
                info("Start updating email")

                val statusCode = emailUpdate.send(uuid)

                isInProgress = false

                if (statusCode == 204) {
                    mAccount?.email = emailStr
                    mListener?.updateEmail(emailStr)

                    current_email.text = emailStr

                    toast(R.string.success_saved)
                } else {
                    toast("API response status: $statusCode")
                }
            } catch (e: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true

                handleApiError(e)

            } catch (e: Exception) {
                isInProgress = false
                isInputAllowed = true

                handleException(e)
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