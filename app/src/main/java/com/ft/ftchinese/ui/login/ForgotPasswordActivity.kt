package com.ft.ftchinese.ui.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityForgotPasswordBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.data.FetchResult
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
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_forgot_password,
        )

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
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.Error -> result.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    if (result.data) {
                        alertLetterSent()
                    } else {
                        toast("邮件发送失败，未知错误！")
                    }
                }
            }
        }

        letterViewModel.verificationResult.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> alertVrfFailure(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    PasswordResetFragment(it.data)
                        .show(supportFragmentManager, "PasswordResetFragment")
                }
            }
        }
    }

    private fun alertLetterSent() {
        AlertDialog.Builder(this)
            .setMessage(R.string.forgot_password_letter_sent)
            .setPositiveButton(R.string.action_ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun alertVrfFailure(msgId: Int) {
        AlertDialog.Builder(this)
            .setMessage(msgId)
            .setPositiveButton(R.string.action_ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun onClickRequestLetter(view: View) {
        letterViewModel.requestCode()
    }

    fun onSubmitForm(view: View) {
        letterViewModel.verifyCode()
    }

    companion object {
        private const val ARG_EMAIL = "arg_email"

        fun start(context: Context?, email: String) {
            val intent = Intent(context, ForgotPasswordActivity::class.java).apply {
                putExtra(ARG_EMAIL, email)
            }

            context?.startActivity(intent)
        }
    }
}
