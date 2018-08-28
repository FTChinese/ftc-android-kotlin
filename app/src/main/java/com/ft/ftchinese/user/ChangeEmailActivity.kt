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
import com.ft.ftchinese.models.User
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.fragment_change_email.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.io.IOException

class ChangeEmailActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return ChangeEmailFragment.newInstance()
    }

    companion object {
        fun start(context: Context?) {
            val intent = Intent(context, ChangeEmailActivity::class.java)
            context?.startActivity(intent)
        }
    }
}

internal class ChangeEmailFragment : Fragment(), AnkoLogger {

    private var user: User? = null
    private var job: Job? = null
    private var listener: OnFragmentInteractionListener? = null

    /**
     * Set progress indicator.
     * By default, the progress bar is not visible and data input and save button is enabled.
     * In case of any error, progress bar should go away and data input and save button should be re-enabled.
     * In case of successfully uploaded data, progress bar should go away but the data input and save button should be disabled to prevent user re-submitting the same data.
     */
    private var isInProgress: Boolean
        get() = !email_save_button.isEnabled
        set(value) {
            listener?.onProgress(value)
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
            listener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        user = User.loadFromPref(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_change_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        current_email.text = if (user?.email.isNullOrBlank()) "未设置" else user?.email

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
        } else if (emailStr == user?.email) {
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

        val uuid = user?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = launch(UI) {
            val emailUpdate = EmailUpdate(emailStr)

            try {
                info("Start updating email")

                emailUpdate.send(uuid)

                isInProgress = false

                // After email is changed successfully, update local instance of User.
                user?.email = emailStr
                // If successfully changed, this email must have not been verified.
                user?.verified = false

                user?.save(context)
            } catch (e: IllegalStateException) {
                isInProgress = false
                isInputAllowed = true
                toast("请求地址错误")
            } catch (e: IOException) {
                isInProgress = false
                isInputAllowed = true
                toast("网络错误")
            } catch (e: JsonSyntaxException) {
                isInProgress = false
                isInputAllowed = true
                toast("无法解析数据")
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

                toast(e.toString())
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun newInstance(): ChangeEmailFragment {
            return ChangeEmailFragment()
        }
    }
}