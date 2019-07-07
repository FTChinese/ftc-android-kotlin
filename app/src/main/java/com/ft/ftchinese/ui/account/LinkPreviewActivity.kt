package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.LoginMethod
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_link_preview.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Show details of account to be bound, show a button to let
 * user to confirm the performance, or just deny accounts merging.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkPreviewActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var linkViewModel: LinkViewModel
    private lateinit var accountViewModel: AccountViewModel

    private var otherAccount: Account? = null

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    private fun enableInput(value: Boolean) {
        start_link_btn.isEnabled = value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_link_preview)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        otherAccount = intent.getParcelableExtra(EXTRA_ACCOUNT)

        linkViewModel = ViewModelProviders.of(this)
                .get(LinkViewModel::class.java)

        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        initUI()

        linkViewModel.linkResult.observe(this, Observer {


            val linkResult = it ?: it ?: return@Observer

            if (linkResult.error != null) {
                toast(linkResult.error)
                showProgress(false)
                enableInput(true)
                return@Observer
            }

            if (linkResult.exception != null) {
                handleException(linkResult.exception)
                showProgress(false)
                enableInput(true)
                return@Observer
            }

            if (!linkResult.success) {
                toast("Unknown error")
                showProgress(false)
                enableInput(true)
                return@Observer
            }

            toast(R.string.prompt_linked)

            if (!isNetworkConnected()) {
                showProgress(false)
                enableInput(true)
                return@Observer
            }

            // Silently refresh account since this is not
            // required.
            val account = sessionManager.loadAccount()

            if (account == null) {
                showProgress(false)
                return@Observer
            }

            toast(R.string.prompt_refreshing)

            accountViewModel.refresh(account)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            showProgress(false)

            val accountResult = it ?: return@Observer

            if (accountResult.success != null) {
                sessionManager.saveAccount(accountResult.success)
            }

            setResult(Activity.RESULT_OK)

            finish()
        })
    }

    // Checks which account is email account and which one is wechat account.
    // The first is email-login account, the second is wechat-oauth account.
    private fun sortAccount(): Pair<Account?, Account?> {
        val loginAccount = sessionManager.loadAccount() ?: return Pair(null, null)

        return when (loginAccount.loginMethod) {
            LoginMethod.EMAIL,
            LoginMethod.MOBILE -> Pair(loginAccount, otherAccount)
            LoginMethod.WECHAT -> Pair(otherAccount, loginAccount)
            else -> Pair(null, null)
        }
    }

    private fun initUI() {

        val (ftcAccount, wxAccount) = sortAccount()

        info("FTC account: $ftcAccount")
        info("Wechat account: $wxAccount")

        if (ftcAccount == null || wxAccount == null) {
            return
        }

        supportFragmentManager.commit {
            replace(R.id.frag_ftc_account, LinkTargetFragment.newInstance(
                    m = ftcAccount.membership,
                    heading = "${getString(R.string.label_ftc_account)}\n${ftcAccount.email}"
            ))

            replace(R.id.frag_wx_account, LinkTargetFragment.newInstance(
                    m = wxAccount.membership,
                    heading = "${getString(R.string.label_wx_account)}\n${wxAccount.wechat.nickname}"
            ))
        }

        // If the two accounts are already bound.
        if (ftcAccount.isEqual(wxAccount)) {
            result_tv.text = getString(R.string.accounts_already_bound)

            enableInput(false)
            return
        }

        // If FTC account is already bound to another wechat.
        if (ftcAccount.isLinked) {
            result_tv.text = getString(R.string.ftc_account_coupled, ftcAccount.email)

            enableInput(false)
            return
        }

        if (wxAccount.isLinked) {
            result_tv.text = getString(R.string.wx_account_coupled, wxAccount.wechat.nickname)

            enableInput(false)
            return
        }

        // Both accounts have memberships and not expired yet.
        if (!ftcAccount.membership.isExpired && !wxAccount.membership.isExpired) {
            result_tv.text = getString(R.string.accounts_member_valid)

            enableInput(false)
            return
        }

        val unionId = wxAccount.unionId ?: return

        start_link_btn.setOnClickListener {
            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            showProgress(true)
            enableInput(false)

            linkViewModel.link(ftcAccount.id, unionId)
        }
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun startForResult(activity: Activity?, account: Account) {
            val intent = Intent(activity, LinkPreviewActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account)
            }

            activity?.startActivityForResult(intent, RequestCode.LINK)
        }
    }
}
