package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
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
    private var isSignUp = false

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment_double)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        viewModel = ViewModelProviders.of(this)
                .get(LoginViewModel::class.java)

        viewModel.inProgress.observe(this, Observer<Boolean> {
            showProgress(it)
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

    private fun onEmailResult(findResult: FindEmailResult?) {

        if (findResult == null) {
            return
        }
        showProgress(false)

        if (findResult.error != null) {
            toast(findResult.error)
            return
        }

        if (findResult.exception != null) {
            handleException(findResult.exception)
            return
        }

        if (findResult.success == null) {
            toast(R.string.loading_failed)
            return
        }

        val (email, found) = findResult.success
        // If email is found, show login ui;
        // otherwise show sign up ui.
        if (found) {
            supportFragmentManager.commit {
                replace(R.id.double_frag_primary, SignInFragment.newInstance(email))
                addToBackStack(null)
            }
        } else {
            isSignUp = true
            supportFragmentManager.commit {
                replace(R.id.double_frag_primary, SignUpFragment.newInstance(email))
                addToBackStack(null)
            }
        }
    }

    private fun onAccountResult(accountResult: AccountResult?) {

        showProgress(false)

        if (accountResult == null) {
            return
        }

        if (accountResult.error != null) {
            toast(accountResult.error)
            return
        }

        if (accountResult.exception != null) {
            handleException(accountResult.exception)
            return
        }

        if (accountResult.success == null) {
            toast(R.string.loading_failed)
            return
        }

        // Is user created a new ftc account, do not show the LinkPreviewActivity since the
        // new account is automatically linked upon creation.
        if (isSignUp) {
            setResult(Activity.RESULT_OK)
            finish()
            return
        }

        LinkPreviewActivity.startForResult(this, accountResult.success)
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
