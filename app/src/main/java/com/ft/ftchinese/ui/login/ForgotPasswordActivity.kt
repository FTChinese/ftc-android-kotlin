package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityForgotPasswordBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ForgotPasswordActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var viewModel: PasswordResetViewModel
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var session: SessionManager

    private var email = ""

    private fun setUIState(s: FormUIState) {
        when (s) {
            FormUIState.Initial -> {
                binding.enableInput = true
                binding.enableButton = false
                binding.inProgress = false
            }
            FormUIState.Ready -> {
                binding.enableInput = true
                binding.enableButton = true
                binding.inProgress = false
            }
            FormUIState.Progress -> {
                binding.enableInput = false
                binding.enableButton = false
                binding.inProgress = true
            }
            FormUIState.Success -> {
                binding.enableInput = false
                binding.enableButton = false
                binding.inProgress = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forgot_password)

        setSupportActionBar(toolbar)

        setUIState(FormUIState.Initial)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        session = SessionManager.getInstance(this)

        // Show the link that opens VerifyResetCodeActivity.
        binding.showVerifyCodeLink = session.loadPasswordResetEmail() != null

        info("Email: ${session.loadPasswordResetEmail()}")

        email = intent.getStringExtra(ARG_EMAIL) ?: ""
        binding.emailInput.text = Editable.Factory.getInstance().newEditable(email)

        viewModel = ViewModelProvider(this)
                .get(PasswordResetViewModel::class.java)

        connectionLiveData.observe(this, Observer {
            viewModel.isNetworkAvailable.value = it
        })
        viewModel.isNetworkAvailable.value = isConnected

        binding.enableInput = true

        // Observing validation results.
        viewModel.formState.observe(this, Observer {
            onValidationResult(it)
        })

        // Validate email upon input
        binding.emailInput.afterTextChanged {
            email = binding.emailInput.text.toString().trim()
            viewModel.emailDataChanged(email)
        }
        // Manually trigger the validation process.
        if (!email.isBlank()) {
            viewModel.emailDataChanged(email)
        }

        // Observing sending email result.
        viewModel.letterResult.observe(this, Observer<Result<Boolean>> {
            onLetterSent(it)
        })

        binding.btnSendEmail.setOnClickListener {
            setUIState(FormUIState.Progress)
            viewModel.requestResettingPassword(email)
        }


        binding.verifyCodeLink.setOnClickListener {
            VerifyResetCodeActivity.startForResult(this, session.loadPasswordResetEmail() ?: "")
        }
    }

    private fun onValidationResult(state: FormState) {
        if (state.error == null) {
            setUIState(FormUIState.Ready)
            return
        }

        setUIState(FormUIState.Initial)
        if (state.field == ControlField.Email) {
            binding.emailInput.apply {
                error = getString(state.error)
                requestFocus()
            }
        }
    }

    private fun onLetterSent(result: Result<Boolean>) {
        // Save the email in case user exit
        // from VerifyResetCodeActivity
        // and later come back.
        session.savePasswordResetEmail(email)

        when (result) {
            is Result.LocalizedError -> {
                setUIState(FormUIState.Ready)
                toast(result.msgId)
            }
            is Result.Error -> {
                setUIState(FormUIState.Ready)
                result.exception.message?.let {msg ->
                    toast(msg)
                }
            }
            is Result.Success -> {
                if (result.data) {
                    setUIState(FormUIState.Success)
                    VerifyResetCodeActivity.startForResult(this, email)
                } else {
                    toast("Sending email failed. Pleas try again")
                    setUIState(FormUIState.Ready)
                }
            }
        }
    }

    // When password is reset successfully, roll back to
    // login ui.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.PASSWORD_RESET) {
            if (resultCode == Activity.RESULT_OK) {
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (email.isBlank()) {
            setUIState(FormUIState.Initial)
        } else {
            setUIState(FormUIState.Ready)
        }
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
