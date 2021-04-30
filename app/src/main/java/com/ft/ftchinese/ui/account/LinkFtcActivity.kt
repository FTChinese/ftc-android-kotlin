package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Lead a wechat user to sign up an FTC account.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkFtcActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var signUpViewModel: SignUpViewModel
    private lateinit var emailViewModel: EmailExistsViewModel
    private lateinit var binding: ActivityFragmentDoubleBinding

    // A flag to determine whether LinkPreviewActivity should be shown.
    // For new sign up, this do not show preview.
    private var isSignUp = false

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
                    // If email exists, show sign in.
                    if (result.data) {
                        supportFragmentManager.commit {
                            replace(
                                R.id.double_frag_primary,
                                SignInFragment.newInstance(),
                            )
                            addToBackStack(null)
                        }
                    } else {
                        // If email does not exist, show sing up.
                        isSignUp = true
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

        loginViewModel.accountResult.observe(this, {
            onAccountResult(it)
        })
    }

    private fun onAccountResult(accountResult: Result<Account>) {

        when (accountResult) {
            is Result.LocalizedError -> toast(accountResult.msgId)
            is Result.Error -> accountResult.exception.message?.let { toast(it) }
            is Result.Success -> {
                // Is user created a new ftc account, do not show the LinkPreviewActivity since the
                // new account is automatically linked upon creation.
                if (isSignUp) {
                    sessionManager.saveAccount(accountResult.data)
                    /**
                     * Unwrapping chain: [AccountActivity] <- current activity
                     */
                    setResult(Activity.RESULT_OK)
                    finish()
                    return
                }

                LinkPreviewActivity.startForResult(this, accountResult.data)
            }
        }
    }

    /**
     * Unwrapping chain:
     * [AccountActivity] <- current activity <- [LinkPreviewActivity]
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
