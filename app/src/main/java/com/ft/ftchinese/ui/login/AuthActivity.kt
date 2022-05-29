package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAuthBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.base.toast
import com.ft.ftchinese.ui.email.EmailExistsFragment
import com.ft.ftchinese.ui.email.EmailViewModel
import com.ft.ftchinese.ui.mobile.MobileFragment
import com.ft.ftchinese.ui.mobile.MobileViewModel
import com.google.android.material.tabs.TabLayoutMediator

private val tabs = listOf(
    "邮箱",
    "手机号码",
    "微信",
)

class AuthActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var emailViewModel: EmailViewModel
    private lateinit var mobileViewModel: MobileViewModel

    private lateinit var binding: ActivityAuthBinding

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
            Log.i(TAG, "Tab title ${tab.text}, position $position")
        }.attach()

        sessionManager = SessionManager.getInstance(this)

        emailViewModel = ViewModelProvider(this)
            .get(EmailViewModel::class.java)

        mobileViewModel = ViewModelProvider(this)
            .get(MobileViewModel::class.java)

        // Setup network
        connectionLiveData.observe(this) {
            emailViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            emailViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()
    }

    private fun setupViewModel() {

        emailViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        mobileViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        // Open sign-in or sing-up depending on email existence.
        emailViewModel.existsResult.observe(this) { result ->
            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.TextError -> toast(result.text)
                is FetchResult.Success -> {
                    if (result.data) {
                        // Show login dialog
                        SignInFragment
                            .forEmailLogin()
                            .show(supportFragmentManager, "EmailLogIn")
                    } else {
                        // Show signup dialog.
                        SignUpFragment
                            .forEmailLogin()
                            .show(supportFragmentManager, "EmailLogIn")
                    }
                }
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
                0 -> EmailExistsFragment.newInstance()
                1 -> MobileFragment.newInstanceForAuth()
                2 -> WxLoginFragment.newInstance()
                else -> EmailExistsFragment.newInstance()
            }
        }
    }

    companion object {
        private const val TAG = "AuthActivity"
    }
}

