package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityResetPasswordBinding
import com.ft.ftchinese.model.reader.PasswordResetter
import com.ft.ftchinese.model.reader.PwResetBearer
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

private const val EXTRA_TOKEN = "extra_password_reset_token"

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ResetPasswordActivity : ScopedAppActivity(), AnkoLogger {
    private lateinit var viewModel: PasswordResetViewModel
    private lateinit var binding: ActivityResetPasswordBinding

    private var password = ""
    private var confirmPassword = ""

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_reset_password)

        setUIState(FormUIState.Initial)

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val bearer = intent.getParcelableExtra<PwResetBearer>(EXTRA_TOKEN)

        if (bearer == null) {
            toast("Invalid password reset token!")
            return
        }

        binding.email = bearer.email

        viewModel = ViewModelProvider(this)
            .get(PasswordResetViewModel::class.java)

        connectionLiveData.observe(this, Observer {
            viewModel.isNetworkAvailable.value = it
        })
        viewModel.isNetworkAvailable.value = isConnected

        // Validation result
        viewModel.formState.observe(this, Observer {
            onValidationResult(it)
        })

        viewModel.resetResult.observe(this, Observer {
            onPasswordReset(it)
        })

        // Validation input.
        binding.inputPassword.afterTextChanged {
            password = it.trim()
            viewModel.passwordDataChanged(password)
        }

        binding.inputConfirmPassword.afterTextChanged {
            confirmPassword = it.trim()
            viewModel.confirmPwDataChanged(confirmPassword)
        }

        // Handle submit.
        binding.btnResetPassword.setOnClickListener {
            if (password != confirmPassword) {
                binding.inputConfirmPassword.error = "两次输入的密码不同"
                binding.inputConfirmPassword.requestFocus()

                return@setOnClickListener
            }

            setUIState(FormUIState.Progress)
            viewModel.resetPassword(PasswordResetter(
                token = bearer.token,
                password = password
            ))
        }
    }

    private fun onValidationResult(state: FormState) {
        if (state.error == null) {
            setUIState(FormUIState.Ready)
            return
        }

        setUIState(FormUIState.Initial)

        when (state.field) {
            ControlField.Password -> {
                binding.inputPassword.apply {
                    error = getString(state.error)
                    requestFocus()
                }
            }
            ControlField.ConfirmPassword -> {
                binding.inputConfirmPassword.apply{
                    error = getString(state.error)
                    requestFocus()
                }
            }
            else -> {}
        }
    }

    private fun onPasswordReset(result: Result<Boolean>) {

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
                if (result.data) {
                    setUIState(FormUIState.Success)
                    toast("Password reset successfully.")
                    setResult(Activity.RESULT_OK)
                    finish()
                } else {
                    setUIState(FormUIState.Ready)
                    toast("Password reset failed. Please retry later")
                }
            }
        }
    }

    companion object {
        fun startForResult(activity: Activity, token: PwResetBearer) {
            activity.startActivityForResult(
                Intent(activity, ResetPasswordActivity::class.java).apply {
                    putExtra(EXTRA_TOKEN, token)
                },
                RequestCode.PASSWORD_RESET
            )
        }
    }
}
