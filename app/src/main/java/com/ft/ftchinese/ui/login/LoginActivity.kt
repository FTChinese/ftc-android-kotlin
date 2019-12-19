package com.ft.ftchinese.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityFragmentDoubleBinding
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.model.StatsTracker
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.base.parseException
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.Existence
import com.ft.ftchinese.viewmodel.LoginViewModel
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LoginActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel
    private lateinit var statsTracker: StatsTracker

    private lateinit var binding: ActivityFragmentDoubleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_fragment_double)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_fragment_double)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        viewModel = ViewModelProvider(this)
                .get(LoginViewModel::class.java)

        statsTracker = StatsTracker.getInstance(this)


        supportFragmentManager.commit {
            replace(R.id.double_frag_primary, EmailFragment.newInstance())
            replace(R.id.double_frag_secondary, WxLoginFragment.newInstance())
        }

        viewModel.inProgress.observe(this, Observer<Boolean> {
            binding.inProgress = it
        })

        viewModel.emailResult.observe(this, Observer {
            onEmailResult(it)
        })

        // Observing both login and sign up.
        viewModel.accountResult.observe(this, Observer {
            onAccountResult(it)
        })
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
                    supportActionBar?.setTitle(R.string.title_login)

                    supportFragmentManager.commit {
                        replace(R.id.double_frag_primary, SignInFragment.newInstance(result.data.value))
                        addToBackStack(null)
                    }

                    return
                }

                supportActionBar?.setTitle(R.string.title_sign_up)

                supportFragmentManager.commit {
                    replace(R.id.double_frag_primary, SignUpFragment.newInstance(result.data.value))
                    addToBackStack(null)
                }
            }
        }
    }

    private fun onAccountResult(result: Result<Account>) {
        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                toast(parseException(result.exception))
            }
            is Result.Success -> {

                sessionManager.saveAccount(result.data)

                statsTracker.setUserId(result.data.id)

                setResult(Activity.RESULT_OK)

                finish()
            }
        }
    }

//    private fun setup() {
//        viewModel.inProgress.observe(this, Observer<Boolean> {
//            showProgress(it)
//        })

//        viewModel.emailResult.observe(this, Observer {
//
//            val findResult = it ?:return@Observer
//
//            showProgress(false)
//
//            if (findResult.error != null) {
//                toast(findResult.error)
//                return@Observer
//            }
//
//            if (findResult.exception != null) {
//                info(findResult.exception)
//                toast(parseException(findResult.exception))
//                return@Observer
//            }
//
//            val (email, found) = findResult.success ?: return@Observer
//
//            if (found) {
//                supportActionBar?.setTitle(R.string.title_login)
//
//                supportFragmentManager.commit {
//                    replace(R.id.double_frag_primary, SignInFragment.newInstance(email))
//                    addToBackStack(null)
//                }
//
//            } else {
//                supportActionBar?.setTitle(R.string.title_sign_up)
//
//                supportFragmentManager.commit {
//                    replace(R.id.double_frag_primary, SignUpFragment.newInstance(email))
//                    addToBackStack(null)
//                }
//            }
//        })

        // Observing both login and sign up.
//        viewModel.accountResult.observe(this, Observer {
//            showProgress(false)
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
//
//            val account = loginResult.success ?: return@Observer
//
//            sessionManager.saveAccount(account)
//
//            statsTracker.setUserId(account.id)
//
//            setResult(Activity.RESULT_OK)
//
//            finish()
//        })
//    }



    companion object {
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, LoginActivity::class.java),
                    RequestCode.SIGN_IN
            )
        }

        fun start(context: Context?) {
            context?.startActivity(Intent(context, LoginActivity::class.java))
        }
    }
}
