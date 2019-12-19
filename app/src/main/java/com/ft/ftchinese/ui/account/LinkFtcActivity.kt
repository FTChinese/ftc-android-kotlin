package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.Existence
import com.ft.ftchinese.viewmodel.LoginViewModel
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Lead a wechat user to sign up an FTC account.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkFtcActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    private lateinit var binding: ActivityFragmentDoubleBinding

    private var isSignUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_fragment_double)

//        setContentView(R.layout.activity_fragment_double)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        viewModel = ViewModelProvider(this)
                .get(LoginViewModel::class.java)

        viewModel.inProgress.observe(this, Observer<Boolean> {
            binding.inProgress = it
        })

        viewModel.emailResult.observe(this, Observer {
            onEmailResult(it)
        })

        viewModel.accountResult.observe(this, Observer {
            onAccountResult(it)
        })

        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailFragment.newInstance())
        }
    }

    private fun onEmailResult(result: Result<Existence>) {

        binding.inProgress = false
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                if (result.data.found) {
                    supportFragmentManager.commit {
                        replace(R.id.double_frag_primary, SignInFragment.newInstance(result.data.value))
                        addToBackStack(null)
                    }

                    return
                }

                isSignUp = true
                supportFragmentManager.commit {
                    replace(R.id.double_frag_primary, SignUpFragment.newInstance(result.data.value))
                    addToBackStack(null)
                }
            }
        }
    }

    private fun onAccountResult(accountResult: Result<Account>) {

        binding.inProgress = false

        when (accountResult) {
            is Result.LocalizedError -> {
                toast(accountResult.msgId)
            }
            is Result.Error -> {
                accountResult.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                // Is user created a new ftc account, do not show the LinkPreviewActivity since the
                // new account is automatically linked upon creation.
                if (isSignUp) {
                    setResult(Activity.RESULT_OK)
                    finish()
                    return
                }

                LinkPreviewActivity.startForResult(this, accountResult.data)
            }
        }
    }

    /**
     * Handle result from [LinkPreviewActivity] with
     * RequestCode.Link
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
