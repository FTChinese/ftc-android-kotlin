package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class BindEmailActivity : AppCompatActivity(),
        OnCredentialsListener,
        AnkoLogger {

    private var job: Job? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }


    override fun onEmailFound(email: String) {

        val transaction = supportFragmentManager
                .beginTransaction()

        transaction.replace(R.id.single_frag_holder, SignInFragment.newInstance(email))
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onEmailNotFound(email: String) {

        val transaction = supportFragmentManager.beginTransaction()

        transaction.replace(R.id.single_frag_holder, SignUpFragment.newInstance(email, HOST_BINDING_ACTIVITY))

        transaction.addToBackStack(null)
        transaction.commit()
    }

    /**
     * Fetch email account.
     */
    override fun onLoggedIn(userId: String) {
        info("Load account for $userId")

        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        onProgress(true)

        /**
         * TODO handle errors.
         */
        try {
            job = GlobalScope.launch(Dispatchers.Main) {
                val ftcAccount = withContext(Dispatchers.IO) {
                    /**
                     * Must load account via /user/account since at this point the two account are still independent of each other.
                     */
                    FtcUser(userId).fetchAccount()
                }

                onProgress(false)

                info("Account: $ftcAccount")

                if (ftcAccount == null) {
                    toast(R.string.error_not_loaded)
                    return@launch
                }

                info("FTC Account: $ftcAccount")

                AccountsMergeActivity.startForResult(this@BindEmailActivity, ftcAccount)
            }
        } catch (e: ClientError) {
            onProgress(false)
            when (e.statusCode) {
                404 -> toast("Account not found")
                else -> handleApiError(e)
            }
        } catch (e: Exception) {
            onProgress(false)
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

    /**
     * Handle wehcat user creating a new FTC account.
     */
    override fun onSignedUp(userId: String) {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        info("Wechat user sign up")

        onProgress(true)

        /**
         * TODO handle errors.
         */
        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val ftcAccount = withContext(Dispatchers.IO) {
                    /**
                     * Refresh account data via /wx/account endpoint so that login method won't be changed.
                     */
                    sessionManager.loadWxSession()?.fetchAccount()
                }

                onProgress(false)

                info("Wx sign up account: $ftcAccount")

                if (ftcAccount == null) {
                    toast(R.string.error_not_loaded)
                    return@launch
                }

                sessionManager.saveAccount(ftcAccount)

                info("Saved wx sign up account. Notify calling activity")

                setResult(Activity.RESULT_OK)
                finish()
            } catch (e: ClientError) {

                onProgress(false)

                when (e.statusCode) {
                    404 -> toast("Account not found")
                    else -> handleApiError(e)
                }

            } catch (e: Exception) {
                onProgress(false)

                handleException(e)
            }

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