package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.EmailUpdate
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.fragment_email.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


internal class EmailFragment : Fragment(), AnkoLogger {

    private var mUser: Account? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    /**
     * Set progress indicator.
     * By default, the progress bar is not visible and data input and save button is enabled.
     * In case of any error, progress bar should go away and data input and save button should be re-enabled.
     * In case of successfully uploaded data, progress bar should go away but the data input and save button should be disabled to prevent mUser re-submitting the same data.
     */
    private var isInProgress: Boolean
        get() = !email_save_button.isEnabled
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean
        get() = email.isEnabled
        set(value) {
            email.isEnabled = value
            email_save_button.isEnabled = value
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            mListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            val userData = it.getString(ARG_USER_DATA)
            mUser = try {
                gson.fromJson<Account>(userData, Account::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        current_email.text = if (mUser?.email.isNullOrBlank()) "未设置" else mUser?.email

        email_save_button.setOnClickListener {
            attemptSave()
        }
    }

    private fun attemptSave() {
        email.error = null
        val emailStr = email.text.toString()

        var cancel = false

        if (emailStr.isBlank()) {
            email.error = getString(R.string.error_field_required)
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_field_required)
            cancel = true
        } else if (emailStr == mUser?.email) {
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

        val uuid = mUser?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = launch(UI) {
            val emailUpdate = EmailUpdate(emailStr)

            try {
                info("Start updating email")

                val userUpdated = emailUpdate.updateAsync(uuid).await()

                isInProgress = false

                mListener?.onUserSession(userUpdated)
                current_email.text = emailStr

                // Notify parent activity to update data.

                toast(R.string.success_saved)

            } catch (e: ErrorResponse) {
                isInProgress = false
                isInputAllowed = true

                when (e.statusCode) {
                    // Should handle duplicate email here.
                    422 -> {
                        toast("邮箱无效或已经被占用")
                    }
                    404 -> {
                        toast("用户不存在")
                    }
                    400 -> {
                        toast("提交了非法的JSON")
                    }
                }

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
        private const val ARG_USER_DATA = "user_data"
        fun newInstance(user: Account?) =
                EmailFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_USER_DATA, gson.toJson(user))
                    }
            }
    }
}