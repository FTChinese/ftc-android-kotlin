package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.PasswordReset
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.activity_forgot_password.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class ForgotPasswordActivity : AppCompatActivity(),
        AnkoLogger {

    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val email = intent.getStringExtra(ARG_EMAIL) ?: return

        email_input.text = Editable.Factory.getInstance().newEditable(email)

        send_email_btn.setOnClickListener {
            val inputEmail = email_input.text.toString().trim()

            val isValid = isEmailValid(inputEmail)

            if (!isValid) {
                return@setOnClickListener
            }

            sendLetter(inputEmail)
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

        return true
    }

    private fun sendLetter(email: String) {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {
            val passwordReset = PasswordReset(email)

            try {
                val done = withContext(Dispatchers.IO) {
                    passwordReset.send()
                }

                if (done) {
                    toast(R.string.prompt_letter_sent)

                    showProgress(false)
                } else {
                    toast("Sending email failed. Pleas try again")
                    showProgress(false)
                    allowInput(true)
                }

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                info("API error response: ${e.message}")
                when (e.statusCode) {
                    // Email is not found
                    404 -> {
                        toast(R.string.api_email_not_found)
                    }
                    else -> {
                        handleApiError(e)
                    }
                }

            } catch (e: Exception) {

                showProgress(false)
                allowInput(true)

                handleException(e)
            }
        }
    }

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }


    private fun allowInput(value: Boolean) {
        send_email_btn.isEnabled = value
        email_input.isEnabled = value
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun start(context: Context?, email: String) {
            val intent = Intent(context, ForgotPasswordActivity::class.java).apply {
                putExtra(ARG_EMAIL, email)
            }

            context?.startActivity(intent)
        }
    }
}

