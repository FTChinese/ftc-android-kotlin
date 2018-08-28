package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.PasswordReset
import com.google.gson.JsonSyntaxException
import kotlinx.android.synthetic.main.fragment_forgot_password.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.support.v4.toast
import java.io.IOException

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

internal class ForgotPasswordFragment : Fragment() {

    private var job: Job? = null
    private var listener: OnFragmentInteractionListener? = null
    private var isInProgress: Boolean
        get() = !reset_letter_button.isEnabled
        set(value) {
            listener?.onProgress(value)

            // If in progress, hide the message telling what to do next.
            if (value) {
                letter_sent_feedback.visibility = View.GONE
            }
        }
    private var isInputAllowed: Boolean
        get() = reset_letter_button.isEnabled
        set(value) {
            reset_letter_button.isEnabled = value
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

        reset_letter_button.setOnClickListener {
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
        } else {
            isInProgress = true

            sendPasswordResetLetter(emailStr)
        }
    }

    private fun sendPasswordResetLetter(emailStr: String) {
        job = launch(UI) {
            val passwordReset = PasswordReset(emailStr)

            try {
                passwordReset.send()

                successState()

            } catch (e: IllegalStateException) {
                failureState()

                toast("请求地址错误")
            } catch (e: IOException) {
                failureState()

                toast("网络错误")
            } catch (e: JsonSyntaxException) {
                failureState()

                toast("无法解析数据")
            } catch (e: ErrorResponse) {
                failureState()

                when (e.statusCode) {
                    422 -> {
                        toast("邮箱无效")
                    }
                    404 -> {
                        toast("邮箱尚未注册")
                    }
                    400 -> {
                        toast("提交了非法的JSON")
                    }
                }

            } catch (e: Exception) {
                failureState()

                toast(e.toString())
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
