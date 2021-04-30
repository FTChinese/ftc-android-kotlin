package com.ft.ftchinese.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityForgotPasswordBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class ForgotPasswordActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var letterViewModel: ForgotPasswordViewModel
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var session: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_forgot_password)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        session = SessionManager.getInstance(this)

        letterViewModel = ViewModelProvider(this)
            .get(ForgotPasswordViewModel::class.java)

        binding.handler = this
        binding.viewModel = letterViewModel
        binding.lifecycleOwner = this

        intent.getStringExtra(ARG_EMAIL)?.let {
            letterViewModel.emailLiveData.value = it
        }

        connectionLiveData.observe(this) {
            letterViewModel.isNetworkAvailable.value = it
        }
        isConnected.let {
            letterViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()
    }

    private fun setupViewModel() {
        letterViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        letterViewModel.counterLiveData.observe(this) {
            binding.requestCode.text = if (it == 0) {
                getString(R.string.mobile_request_code)
            } else {
                getString(R.string.mobile_code_counter, it)
            }
        }

        // Observing sending email result.
        letterViewModel.letterSent.observe(this) { result ->
            when (result) {
                is Result.LocalizedError -> toast(result.msgId)
                is Result.Error -> result.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    if (result.data) {
                        toast("邮件已发送")
                    } else {
                        toast("邮件发送失败，未知错误！")
                    }
                }
            }
        }

        letterViewModel.verificationResult.observe(this) {
            when (it) {
                is Result.LocalizedError -> toast(it.msgId)
                is Result.Error -> it.exception.message?.let { msg -> toast(msg) }
                is Result.Success -> {
                    PasswordResetFragment(it.data)
                        .show(supportFragmentManager, "PasswordResetFragment")
                }
            }
        }
    }

    fun onClickRequestLetter(view: View) {
        letterViewModel.requestCode()
    }

    fun onSubmitForm(view: View) {
        letterViewModel.verifyCode()
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
