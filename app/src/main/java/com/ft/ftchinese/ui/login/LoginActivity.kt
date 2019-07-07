package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LoginActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
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
            val findResult = it ?:return@Observer

            showProgress(false)

            if (findResult.error != null) {
                toast(findResult.error)
                return@Observer
            }

            if (findResult.exception != null) {
                info(findResult.exception)
                handleException(findResult.exception)
                return@Observer
            }

            val (email, found) = findResult.success ?: return@Observer

            if (found) {
                supportActionBar?.setTitle(R.string.title_login)

                supportFragmentManager.commit {
                    replace(R.id.double_frag_primary, SignInFragment.newInstance(email))
                    addToBackStack(null)
                }

            } else {
                supportActionBar?.setTitle(R.string.title_sign_up)

                supportFragmentManager.commit {
                    replace(R.id.double_frag_primary, SignUpFragment.newInstance(email))
                    addToBackStack(null)
                }
            }
        })

        // Observing both login and sign up.
        viewModel.accountResult.observe(this, Observer {
            showProgress(false)

            val loginResult = it ?: return@Observer

            if (loginResult.error != null) {
                toast(loginResult.error)
                return@Observer
            }

            if (loginResult.exception != null) {
                handleException(loginResult.exception)
                return@Observer
            }

            val account = loginResult.success ?: return@Observer

            sessionManager.saveAccount(account)

            setResult(Activity.RESULT_OK)

            finish()
        })

        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailFragment.newInstance())
            replace(R.id.double_frag_secondary, WxLoginFragment.newInstance())
        }
    }

    companion object {
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, LoginActivity::class.java),
                    RequestCode.SIGN_IN
            )
        }
    }
}
