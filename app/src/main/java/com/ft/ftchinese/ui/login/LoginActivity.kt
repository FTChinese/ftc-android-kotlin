package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

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

        viewModel = ViewModelProviders.of(this)
                .get(LoginViewModel::class.java)

        viewModel.email.observe(this, Observer<Pair<String, Boolean>> {
            val (email, found) = it

            showProgress(false)

            if (found) {
                supportActionBar?.setTitle(R.string.title_login)
                supportFragmentManager.beginTransaction()
                        .replace(R.id.double_frag_primary, SignInFragment.newInstance(email))
                        .addToBackStack(null)
                        .commit()
            } else {
                supportActionBar?.setTitle(R.string.title_sign_up)

                supportFragmentManager.beginTransaction()
                        .replace(R.id.double_frag_primary, SignUpFragment.newInstance(email, HOST_SIGN_UP_ACTIVITY))
                        .addToBackStack(null)
                        .commit()
            }
        })

        viewModel.userId.observe(this, Observer<String> {
            loadAccount(it)
        })

        viewModel.inProgress.observe(this, Observer<Boolean> {
            showProgress(it)
        })

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.double_frag_primary, EmailFragment.newInstance())
                .replace(R.id.double_frag_secondary, WxLoginFragment.newInstance())
                .commit()

        sessionManager = SessionManager.getInstance(this)
    }

    private fun loadAccount(userId: String) {

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