package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LoginActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var statsTracker: StatsTracker

    private lateinit var binding: ActivityFragmentDoubleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_fragment_double)

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        loginViewModel = ViewModelProvider(this)
            .get(LoginViewModel::class.java)

        emailViewModel = ViewModelProvider(this)
            .get(EmailExistsViewModel::class.java)

        signUpViewModel = ViewModelProvider(this)
            .get(SignUpViewModel::class.java)

        // Setup network
        connectionLiveData.observe(this) {
            loginViewModel.isNetworkAvailable.value = it
            emailViewModel.isNetworkAvailable.value = it
            signUpViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            emailViewModel.isNetworkAvailable.value = it
            loginViewModel.isNetworkAvailable.value = it
            signUpViewModel.isNetworkAvailable.value = it
        }

        // Analytics
        statsTracker = StatsTracker.getInstance(this)

        setupViewModel()

        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailFragment.newInstance())
            replace(R.id.double_frag_secondary, WxLoginFragment.newInstance())
        }
    }

    private fun setupViewModel() {
        loginViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }
        signUpViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }
        emailViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        emailViewModel.existsResult.observe(this) { result ->
            when (result) {
                is Result.LocalizedError -> toast(result.msgId)
                is Result.Error -> result.exception.message?.let { toast(it) }
                is Result.Success -> {

                    if (result.data) {
                        supportActionBar?.setTitle(R.string.title_login)

                        supportFragmentManager.commit {
                            replace(
                                R.id.double_frag_primary,
                                SignInFragment.newInstance(),
                            )
                            addToBackStack(null)
                        }
                    } else {
                        supportActionBar?.setTitle(R.string.title_sign_up)

                        supportFragmentManager.commit {
                            replace(
                                R.id.double_frag_primary,
                                SignUpFragment.newInstance(),
                            )
                            addToBackStack(null)
                        }
                    }
                }
            }
        }

        // Observing both login and sign up.
        loginViewModel.accountResult.observe(this, this::onAccountResult)

        signUpViewModel.accountResult.observe(this, this::onAccountResult)
    }

    private fun onAccountResult(result: Result<Account>) {

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {

                sessionManager.saveAccount(result.data)

                statsTracker.setUserId(result.data.id)

                setResult(Activity.RESULT_OK)

                finish()
            }
        }
    }

    companion object {
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, LoginActivity::class.java),
                    RequestCode.SIGN_IN
            )
        }

        fun start(context: Context?) {
            context?.startActivity(Intent(context, LoginActivity::class.java))
        }
    }
}
