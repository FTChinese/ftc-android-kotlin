package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.FtcUser
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.ui.login.*
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkEmailActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

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

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.single_frag_holder, EmailFragment.newInstance())
                .commit()

        viewModel = ViewModelProviders.of(this)
                .get(LoginViewModel::class.java)

        viewModel.email.observe(this, Observer<Pair<String, Boolean>> {
            val (email, found) = it
            showProgress(false)

            if (found) {
                supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.single_frag_holder, SignInFragment.newInstance(email))
                        .addToBackStack(null)
                        .commit()
            } else {
                supportFragmentManager.beginTransaction()
                        .replace(R.id.single_frag_holder, SignUpFragment.newInstance(email, HOST_BINDING_ACTIVITY))
                        .addToBackStack(null)
                        .commit()
            }
        })

        viewModel.inProgress.observe(this, Observer<Boolean> {
            showProgress(it)
        })

        viewModel.userId.observe(this, Observer<String> {
            loadAccount(it)
        })
    }

    private fun loadAccount(userId: String) {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        showProgress(true)

        try {
            launch {
                val ftcAccount = withContext(Dispatchers.IO) {
                    /**
                     * Must load account via /user/account since at this point the two account are still independent of each other.
                     */
                    FtcUser(userId).fetchAccount()
                }

                viewModel.showProgress(false)
                info("Account: $ftcAccount")

                if (ftcAccount == null) {
                    toast(R.string.error_not_loaded)
                    return@launch
                }

                info("FTC Account: $ftcAccount")

                LinkActivity.startForResult(this@LinkEmailActivity, ftcAccount)
            }

        } catch (e: ClientError) {
            viewModel.showProgress(false)
            when (e.statusCode) {
                404 -> toast("Account not found")
                else -> handleApiError(e)
            }
        } catch (e: Exception) {
            viewModel.showProgress(false)
            handleException(e)
        }
    }

    /**
     * Handle result from AccountsMergeActivity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult: requestCode $requestCode, $resultCode")

        if (requestCode != RequestCode.BOUND) {
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
                    Intent(activity, LinkEmailActivity::class.java),
                    requestCode
            )

        }
    }
}
