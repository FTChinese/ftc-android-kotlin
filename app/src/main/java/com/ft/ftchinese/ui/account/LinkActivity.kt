package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.*
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    private var isSignUp = false

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContentView(R.layout.activity_fragment_single)
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
            val findResult = it ?: return@Observer

            showProgress(false)

            if (findResult.error != null) {
                toast(findResult.error)

                return@Observer
            }

            if (findResult.exception != null) {
                handleException(findResult.exception)
                return@Observer
            }

            if (findResult.success == null) {
                toast(R.string.error_not_loaded)
                return@Observer
            }

            val (email, found) = findResult.success
            // If email is found, show login ui;
            // otherwise show sign up ui.
            if (found) {
                supportFragmentManager.commit {
                    replace(R.id.single_frag_holder, SignInFragment.newInstance(email))
                    addToBackStack(null)
                }
            } else {
                isSignUp = true
                supportFragmentManager.commit {
                    replace(R.id.single_frag_holder, SignUpFragment.newInstance(email))
                    addToBackStack(null)
                }
            }
        })

        viewModel.accountResult.observe(this, Observer {
            val loginResult = it ?: return@Observer

            if (loginResult.error != null) {
                toast(loginResult.error)
                return@Observer
            }

            if (loginResult.exception != null) {
                handleException(loginResult.exception)
                return@Observer
            }

            if (loginResult.success == null) {
                toast(R.string.error_not_loaded)
                return@Observer
            }

            if (isSignUp) {
                setResult(Activity.RESULT_OK)
                finish()
                return@Observer
            }

            LinkPreviewActivity.startForResult(this@LinkActivity, loginResult.success)

        })


        supportFragmentManager.commit {
            replace(R.id.single_frag_holder, EmailFragment.newInstance())
        }
    }

    /**
     * Handle result from [LinkPreviewActivity]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult: requestCode $requestCode, $resultCode")

        if (requestCode != RequestCode.LINK) {
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        info("Bound to an existing ftc account")
        /**
         * Pass result to WxAccountFragment.
         */
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        @JvmStatic
        fun startForResult(activity: Activity?, requestCode: Int) {
            activity?.startActivityForResult(
                    Intent(activity, LinkActivity::class.java),
                    requestCode
            )

        }
    }
}
