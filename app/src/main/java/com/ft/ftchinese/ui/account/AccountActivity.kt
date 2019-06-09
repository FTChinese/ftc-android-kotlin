package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.models.LoginMethod
import com.ft.ftchinese.models.SessionManager
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
class AccountActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AccountViewModel

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

        initUI()

        viewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        viewModel.inProgress.observe(this, Observer<Boolean> {
            showProgress(it)
        })

        viewModel.loginMethod.observe(this, Observer<LoginMethod> {
            initUI()
        })
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        val fm = supportFragmentManager
                .beginTransaction()

        if (account.isWxOnly) {
            fm.replace(R.id.single_frag_holder, WxFragment.newInstance())
        } else {
            fm.replace(R.id.single_frag_holder, FtcFragment.newInstance())
        }

        fm.commit()
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}
