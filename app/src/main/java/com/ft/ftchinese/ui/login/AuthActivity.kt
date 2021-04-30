package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAuthBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.Result
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

private val tabs = listOf(
    "邮箱",
    "手机号码",
    "微信",
)

@ExperimentalCoroutinesApi
class AuthActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var binding: ActivityAuthBinding

    private lateinit var statsTracker: StatsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_auth)

        setSupportActionBar(binding.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        binding.authViewPager.adapter = Adapter(this)

        TabLayoutMediator(binding.authTabLayout, binding.authViewPager) { tab, position ->
            tab.text = tabs[position]
            info("Tab title ${tab.text}, position $position")
        }.attach()

        sessionManager = SessionManager.getInstance(this)

        emailViewModel = ViewModelProvider(this)
            .get(EmailExistsViewModel::class.java)

        loginViewModel = ViewModelProvider(this)
            .get(LoginViewModel::class.java)

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

        setupViewModel()

        // Analytics
        statsTracker = StatsTracker.getInstance(this)
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

        emailViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        emailViewModel.existsResult.observe(this) { result ->
            when (result) {
                is Result.LocalizedError -> toast(result.msgId)
                is Result.Error -> result.exception.message?.let { toast(it) }
                is Result.Success -> {
                    if (result.data) {
                        // Show login dialog
                        SignInFragment()
                            .show(supportFragmentManager, "SignInDialog")
                    } else {
                        // Show signup dialog.
                        SignUpFragment()
                            .show(supportFragmentManager, "SignUpDialog")
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

    override fun onBackPressed() {
        if (binding.authViewPager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.authViewPager.apply {
                currentItem -= 1
            }
        }
    }

    inner class Adapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

        override fun getItemCount(): Int = tabs.size

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> EmailFragment.newInstance()
                1 -> MobileAuthFragment.newInstance()
                2 -> WxLoginFragment.newInstance()
                else -> EmailFragment.newInstance()
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, AuthActivity::class.java))
        }

        @JvmStatic
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                Intent(activity, AuthActivity::class.java),
                RequestCode.SIGN_IN
            )
        }
    }
}
