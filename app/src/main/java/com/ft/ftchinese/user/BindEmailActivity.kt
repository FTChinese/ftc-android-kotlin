package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.WxSessionManager
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

class BindEmailActivity : AppCompatActivity(),
        OnCredentialsListener,
        AnkoLogger {

    private var job: Job? = null
    private var sessionManager: SessionManager? = null
    private var wxSessionManager: WxSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, EmailFragment.newInstance())
                .commit()

        sessionManager = SessionManager.getInstance(this)
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onLogIn(email: String) {

        val transaction = supportFragmentManager
                .beginTransaction()

        transaction.replace(R.id.fragment_container, SignInFragment.newInstance(email))
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onSignUp(email: String) {

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, SignUpFragment.newInstance(email))
        transaction.addToBackStack(null)
        transaction.commit()
    }

    /**
     * Fetch email account.
     */
    override fun onLoadAccount(userId: String) {
        info("Load account for $userId")

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        job = GlobalScope.launch(Dispatchers.Main) {
            val ftcAccount = withContext(Dispatchers.IO) {
                FtcUser(userId).fetchAccount()
            }

            info("Account: $ftcAccount")

            if (ftcAccount == null) {
                toast(R.string.error_not_loaded)
                return@launch
            }

            info("FTC Account: $ftcAccount")


        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun startForResult(activity: Activity?, requestCode: Int) {
            activity?.startActivityForResult(
                    Intent(activity, BindEmailActivity::class.java),
                    requestCode
            )
        }
    }
}