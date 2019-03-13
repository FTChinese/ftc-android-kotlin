package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class CredentialsActivity : AppCompatActivity(),
        OnCredentialsListener,
        AnkoLogger {

    private var sessionManager: SessionManager? = null
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment_double)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.double_frag_primary, EmailFragment.newInstance())
                .replace(R.id.double_frag_secondary, WxLoginFragment.newInstance())
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

    override fun onEmailFound(email: String) {
        info("Login for email $email")
        supportActionBar?.setTitle(R.string.title_login)

        val transaction = supportFragmentManager
                .beginTransaction()

        transaction.replace(R.id.double_frag_primary, SignInFragment.newInstance(email))
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onEmailNotFound(email: String) {
        toast("Sign up for email $email")
        supportActionBar?.setTitle(R.string.title_sign_up)

        val transaction = supportFragmentManager
                .beginTransaction()

        transaction.replace(R.id.double_frag_primary, SignUpFragment.newInstance(email, HOST_SIGN_UP_ACTIVITY))

        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onLoggedIn(userId: String) {

        loadAccount(userId)
    }

    override fun onSignedUp(userId: String) {
        loadAccount(userId)
    }

    private fun loadAccount(userId: String) {
        val user = FtcUser(id = userId)

        toast(R.string.progress_fetching)
        onProgress(true)

        job = GlobalScope.launch(Dispatchers.Main) {
            val account = withContext(Dispatchers.IO) {
                user.fetchAccount()
            }

            onProgress(false)

            if (account == null) {
                toast(R.string.error_not_loaded)
                return@launch
            }

            sessionManager?.saveAccount(account)

            setResult(Activity.RESULT_OK)

            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, CredentialsActivity::class.java),
                    RequestCode.SIGN_IN
            )
        }
    }
}