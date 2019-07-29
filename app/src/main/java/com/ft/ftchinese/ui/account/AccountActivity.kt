package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.LoginMethod
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

/**
 * Show user's account details.
 * Show different fragments based on whether FTC account is bound to wechat account.
 * If user logged in with email account, show FtcAccountFramgnet;
 * If user logged in with wechat account and it is not bound to an FTC account, show WxAccountFragment;
 * If user logged in with wechat account and it is bound to an FTC account, show FtcAccountFragment.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class AccountActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AccountViewModel

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        initUI()

        viewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        viewModel.inProgress.observe(this, Observer<Boolean> {
            showProgress(it)
        })

        viewModel.uiType.observe(this, Observer<LoginMethod> {
            initUI()
        })

        viewModel.shouldReAuth.observe(this, Observer {
            if (it) {

                WxExpireDialogFragment()
                        .show(supportFragmentManager, "WxExpireDialog")

                sessionManager.logout()
            }
        })
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        supportFragmentManager.commit {
            if (account.isWxOnly) {
                replace(R.id.frag_account, WxInfoFragment.newInstance())
            } else {
                replace(R.id.frag_account, FtcAccountV2Fragment.newInstance())
            }
        }
    }

    /**
     * Receive results from [UpdateActivity] or [LinkFtcActivity]
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        // If user linked accounts, reload ui.
        if (requestCode == RequestCode.LINK) {
            initUI()
            return
        }

        // If user unlinked accounts, reload ui.
        if (requestCode == RequestCode.UNLINK) {
            initUI()
        }
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}
