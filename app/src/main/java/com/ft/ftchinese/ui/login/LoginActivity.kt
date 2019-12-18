package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.model.StatsTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.base.parseException
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.LoginViewModel
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LoginActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    private lateinit var statsTracker: StatsTracker

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

        viewModel = ViewModelProvider(this)
                .get(LoginViewModel::class.java)


        statsTracker = StatsTracker.getInstance(this)

        setup()
        initUI()
    }

    private fun initUI() {
        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailFragment.newInstance())
            replace(R.id.double_frag_secondary, WxLoginFragment.newInstance())
        }
    }

    private fun setup() {
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
                toast(parseException(findResult.exception))
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

            when (it) {
                is Result.LocalizedError -> {
                    toast(it.msgId)
                }
                is Result.Error -> {
                    toast(parseException(it.exception))
                }
                is Result.Success -> {

                    sessionManager.saveAccount(it.data)

                    statsTracker.setUserId(it.data.id)

                    setResult(Activity.RESULT_OK)

                    finish()
                }
            }
//            val loginResult = it ?: return@Observer
//
//            if (loginResult.error != null) {
//                toast(loginResult.error)
//                return@Observer
//            }
//
//            if (loginResult.exception != null) {
//                toast(parseException(loginResult.exception))
//                return@Observer
//            }

//            val account = loginResult.success ?: return@Observer
//
//            sessionManager.saveAccount(account)
//
//            statsTracker.setUserId(account.id)
//
//            setResult(Activity.RESULT_OK)
//
//            finish()
        })
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
