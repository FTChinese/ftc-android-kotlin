package com.ft.ftchinese.ui.wxlink

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.RequestCode
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * UI for a wx-only user to link to an email account.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkFtcActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: SignInViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var binding: ActivityFragmentDoubleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_fragment_double,
        )

        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        loginViewModel = ViewModelProvider(this)
            .get(SignInViewModel::class.java)

        emailViewModel = ViewModelProvider(this)
            .get(EmailExistsViewModel::class.java)

        signUpViewModel = ViewModelProvider(this)
            .get(SignUpViewModel::class.java)

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

        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailFragment.newInstance())
        }
    }

    private fun setupViewModel() {

        emailViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        loginViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        signUpViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        emailViewModel.existsResult.observe(this) { result ->
            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.Error -> result.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    // If email exists, show sign in.
                    if (result.data) {
                        SignInFragment()
                            .show(supportFragmentManager, "WxLinkEmailSignIn")
                    } else {
                        // If email does not exist, show sing up.
                        SignUpFragment()
                            .show(supportFragmentManager, "WxLinkEmailSignUp")
                    }
                }
            }
        }

        // Account acquired from email login.
        loginViewModel.accountResult.observe(this) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    // Show preview UI.
                    LinkPreviewFragment()
                        .show(supportFragmentManager, "WxLinkEmailPreview")
                }
            }
        }

        signUpViewModel.accountResult.observe(this) {
           when (it) {
               is FetchResult.LocalizedError -> toast(it.msgId)
               is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
               is FetchResult.Success -> {
                   toast(R.string.prompt_linked)
                   sessionManager.saveAccount(it.data)
                   /**
                    * If user created a new ftc account,
                    * it is automatically linked upon creation.
                    * back to [AccountActivity]
                    */
                   setResult(Activity.RESULT_OK)
                   finish()
               }
           }
        }
    }

    /**
     * Unwrapping chain:
     * [AccountActivity] <- current activity <- [LinkPreviewFragment]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult: requestCode $requestCode, $resultCode")

        if (requestCode != RequestCode.LINK) {
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            setResult(Activity.RESULT_CANCELED)
            return
        }

        info("Bound to an existing ftc account")
        /**
         * Pass data back to [AccountActivity]
         */
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        @JvmStatic
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, LinkFtcActivity::class.java),
                    RequestCode.LINK
            )

        }
    }
}
