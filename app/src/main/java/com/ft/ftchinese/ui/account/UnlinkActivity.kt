package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.UnlinkAnchor
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_unlink.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

class UnlinkActivity : AppCompatActivity() {

    private lateinit var accountViewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var linkViewModel: LinkViewModel
    private var anchor: UnlinkAnchor? = null

    private fun showProgress(show: Boolean) {
        progress_bar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlink)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        linkViewModel = ViewModelProviders.of(this)
                .get(LinkViewModel::class.java)

        initUI()

        linkViewModel.anchorSelected.observe(this, Observer {
                anchor = it
        })

        linkViewModel.unlinkResult.observe(this, Observer {

            showProgress(false)

            val unlinkResult = it ?: return@Observer
            if (unlinkResult.error != null) {
                toast(unlinkResult.error)
                unlink_button.isEnabled = true
                return@Observer
            }

            if (unlinkResult.exception != null) {
                handleException(unlinkResult.exception)
                unlink_button.isEnabled = true
                return@Observer
            }

            if (!unlinkResult.success) {
                toast("Unknown error occurred")
                unlink_button.isEnabled = true
                return@Observer
            }

            toast(R.string.prompt_unlinked)

            val acnt = sessionManager.loadAccount() ?: return@Observer

            // Start refreshing account.
            showProgress(true)

            toast(R.string.refreshing_data)
            accountViewModel.refresh(acnt)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            showProgress(false)

            val accountResult = it ?: return@Observer

            if (accountResult.error != null) {
                toast(accountResult.error)
                return@Observer
            }

            if (accountResult.exception != null) {
                handleException(accountResult.exception)
                return@Observer
            }

            if (accountResult.success == null) {
                toast("Unknown error")
                return@Observer
            }

            toast(R.string.prompt_updated)

            sessionManager.saveAccount(accountResult.success)

            // Signal to calling activity.
            setResult(Activity.RESULT_OK)
            finish()
        })

        unlink_button.setOnClickListener {
            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            if (account.isMember && anchor == null) {
                toast(R.string.api_anchor_missing)
                return@setOnClickListener
            }

            progress_bar.visibility = View.VISIBLE
            unlink_button.isEnabled = false

            linkViewModel.unlink(account, anchor)
        }
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        unlink_ftc_account.text = arrayOf(getString(R.string.label_ftc_account), account.email).joinToString("\n")
        unlink_wx_account.text = arrayOf(getString(R.string.label_wx_account), account.wechat.nickname).joinToString("\n")

        if (account.isMember) {
            supportFragmentManager.commit {
                replace(
                        R.id.frag_unlink_anchor,
                        UnlinkAnchorFragment.newInstance(
                                account.membership.payMethod == PayMethod.STRIPE
                        )
                )
            }
        }
    }

    companion object {
        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                    Intent(activity, UnlinkActivity::class.java),
                    RequestCode.UNLINK
            )
        }
    }
}


