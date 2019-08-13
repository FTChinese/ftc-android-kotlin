package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.SessionManager
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

        viewModel = ViewModelProvider(this)
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
     * Receive results from
     * [UpdateActivity]
     * [LinkFtcActivity] with RequestCode.Link, which is
     * received from [LinkPreviewActivity]
     * [WxInfoActivity] with RequestCode.Unlink, which is
     * received from [UnlinkActivity].
     * In cases this activity starts [WxInfoActivity] with
     * wechat account not linked, and user starts wechat OAuth,
     * the request code mechanism does not work since WxEntryActivity interferes with this flow.
     * In such case the [onResume] is the last resort to
     * ensure UI changes.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        // If user linked accounts, reload ui.
        if (requestCode == RequestCode.LINK || requestCode == RequestCode.UNLINK) {
            initUI()
        }
    }

    /**
     * This ensures UI changes as user link/unlink accounts.
     * The onActivityResult mechanism is not reliable since
     * Wechat's WXEntryActivity might interrupt the data
     * passing back.
     */
    override fun onResume() {
        super.onResume()
        initUI()
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}
