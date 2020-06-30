package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityVerifyResetCodeBinding
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.model.reader.PwResetVerifier
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

private const val EXTRA_EMAIL= "extra_email"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class VerifyResetCodeActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var viewModel: PasswordResetViewModel
    private lateinit var binding: ActivityVerifyResetCodeBinding
    private lateinit var session: SessionManager

    private var code = ""
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_verify_reset_code)

        setSupportActionBar(toolbar)

        setUIState(FormUIState.Initial)

        session = SessionManager.getInstance(this)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: ""

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        viewModel = ViewModelProvider(this)
            .get(PasswordResetViewModel::class.java)

        connectionLiveData.observe(this, Observer {
            viewModel.isNetworkAvailable.value = it
        })
        viewModel.isNetworkAvailable.value = isConnected

        viewModel.formState.observe(this, Observer {
            onValidationResult(it)
        })

        viewModel.verificationResult.observe(this, Observer {
            onCodeVerified(it)
        })

        binding.inputCode.afterTextChanged {
            code = it.trim()
            viewModel.codeDataChanged(code)
        }

        binding.btnVerifyCode.setOnClickListener {
            setUIState(FormUIState.Progress)
            if (email.isBlank()) {
                toast("The email to reset password is not found. Please go back and request a password reset email.")
                return@setOnClickListener
            }
            viewModel.verifyPwResetCode(PwResetVerifier(
                email = email,
                code = code
            ))
        }
    }

    private fun onValidationResult(state: FormState) {
        if (state.error == null) {
            setUIState(FormUIState.Ready)
            return
        }

        setUIState(FormUIState.Initial)
        if (state.field == ControlField.PasswordResetCode) {
            binding.inputCode.apply {
                error = getString(state.error)
                requestFocus()
            }
        }
    }

    private fun onCodeVerified(result: Result<PwResetBearer>) {
        session.clearPasswordResetEmail()
        when (result) {
            is Result.LocalizedError -> {
                setUIState(FormUIState.Ready)
                toast(result.msgId)
            }
            is Result.Error -> {
                setUIState(FormUIState.Ready)
                result.exception.message?.let {
                    toast(it)
                }
            }
            is Result.Success -> {
                ResetPasswordActivity.startForResult(this, result.data)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RequestCode.PASSWORD_RESET) {
            if (resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    companion object {
        fun startForResult(activity: Activity, email: String) {
            activity.startActivityForResult(
                Intent(activity, VerifyResetCodeActivity::class.java).apply {
                    putExtra(EXTRA_EMAIL, email)
                },
                RequestCode.PASSWORD_RESET
            )
        }
    }
}
