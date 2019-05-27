package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.fragment.app.Fragment
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info


/**
 * Show user's account details.
 * Show different fragments based on whether FTC account is bound to wechat account.
 * If user logged in with email account, show FtcAccountFramgnet;
 * If user logged in with wechat account and it is not bound to an FTC account, show WxAccountFragment;
 * If user logged in with wechat account and it is bound to an FTC account, show FtcAccountFragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class AccountActivity : AppCompatActivity(),
        OnSwitchAccountListener,
        AnkoLogger {

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

        initUI()
        info("AccountActivity.onCreate")
    }

    /**
     * Select the fragment to attach based on whether
     * FTC account is bound to wechat account,
     * and the login method.
     */
    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        info("Account: $account")

        val fragment: Fragment = if (account.isWxOnly) {
            info("Using WxAccountFragment")
            WxAccountFragment.newInstance()
        } else {
            info("Using FtcAccountFragment")
            FtcAccountFragment.newInstance()
        }

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.single_frag_holder, fragment)
                .commit()
    }

    override fun onProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onSwitchAccount() {
        initUI()
    }

    /**
     * Receive message from BindEmailActivity to reattach fragment.
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

        initUI()
    }



    companion object {

        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

