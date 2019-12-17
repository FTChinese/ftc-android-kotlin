package com.ft.ftchinese.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityForgotPasswordBinding
import com.ft.ftchinese.model.Result
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.viewmodel.LoginViewModel
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ForgotPasswordActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_forgot_password)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forgot_password)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        viewModel = ViewModelProvider(this)
                .get(LoginViewModel::class.java)

        binding.enableInput = true

        // Observing validation results.
        viewModel.loginFormState.observe(this, Observer {
            binding.btnSendEmail.isEnabled = it.isEmailValid

            if (it.error != null) {
                binding.emailInput.error = getString(it.error)
                binding.emailInput.requestFocus()
            }
        })

        val email = intent.getStringExtra(ARG_EMAIL)
        binding.emailInput.text = Editable.Factory.getInstance().newEditable(email)

        // Validate email upon input
        viewModel.emailDataChanged(email ?: "")
        binding.emailInput.afterTextChanged {
            viewModel.emailDataChanged(binding.emailInput.text.toString().trim())
        }

        binding.btnSendEmail.setOnClickListener {
            val inputEmail = binding.emailInput.text.toString().trim()


            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            binding.enableInput = false
            binding.inProgress = true

            viewModel.requestResettingPassword(inputEmail)
        }

        // Observing sending email result.
        viewModel.pwResetLetterResult.observe(this, Observer<Result<Boolean>> {

            binding.inProgress = false

            when (it) {
                is Result.Success -> {
                    if (it.data) {
                        binding.emailSent = true
                    } else {
                        toast("Sending email failed. Pleas try again")
                        binding.enableInput = true
                    }
                }
                is Result.LocalizedError -> {
                    binding.enableInput = true
                    toast(it.msgId)
                }
                is Result.Error -> {
                    binding.enableInput = true
                    toast(parseException(it.exception))
                }
            }
        })
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
