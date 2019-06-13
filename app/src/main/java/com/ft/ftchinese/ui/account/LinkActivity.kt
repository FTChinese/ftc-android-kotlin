package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.FtcUser
import com.ft.ftchinese.model.LoginMethod
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.activity_accounts_merge.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Show details of account to be bound, show a button to let
 * user to confirm the performance, or just deny accounts merging.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LinkActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private var otherAccount: Account? = null

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    private fun allowInput(value: Boolean) {
        start_binding_btn.isEnabled = value
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts_merge)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        otherAccount = intent.getParcelableExtra(EXTRA_ACCOUNT)

        updateUI()
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

    private fun updateUI() {

        val (ftcAccount, wxAccount) = sortAccount()

        info("FTC account: $ftcAccount")
        info("Wechat account: $wxAccount")

        if (ftcAccount == null || wxAccount == null) {
            return
        }

        val ftcMemberFrag = MemberFragment.newInstance(
                m = ftcAccount.membership,
                heading = "FT中文网账号\n${ftcAccount.email}"
        )
        val wxMemberFrag = MemberFragment.newInstance(
                m = wxAccount.membership,
                heading = "微信账号\n${wxAccount.wechat.nickname}"
        )

        supportFragmentManager.beginTransaction()
                .replace(R.id.frag_ftc_account, ftcMemberFrag)
                .replace(R.id.frag_wx_account, wxMemberFrag)
                .commit()


        // If the two accounts are already bound.
        if (ftcAccount.isEqual(wxAccount)) {
            result_tv.text = getString(R.string.accounts_already_bound)

            allowInput(false)
            return
        }

        // If FTC account is already bound to another wechat.
        if (ftcAccount.isLinked) {
            result_tv.text = getString(R.string.ftc_account_coupled, ftcAccount.email)

            allowInput(false)
            return
        }

        if (wxAccount.isLinked) {
            result_tv.text = getString(R.string.wx_account_coupled, wxAccount.wechat.nickname)

            allowInput(false)
            return
        }

        // Both accounts have memberships and not expired yet.
        if (!ftcAccount.membership.isExpired && !wxAccount.membership.isExpired) {
            result_tv.text = getString(R.string.accounts_member_valid)

            allowInput(false)
            return
        }

        start_binding_btn.setOnClickListener {
            link(ftcAccount.id, wxAccount.unionId)
        }
    }

    private fun link(userId: String, unionId: String?) {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        if (unionId == null) {
            toast("Wechat union id not found")
            return
        }

        showProgress(true)
        allowInput(false)

        launch {
            try {
                val done = withContext(Dispatchers.IO) {

                    FtcUser(userId)
                            .bindWechat(unionId)
                }

                toast(R.string.prompt_bound)

                info("Bind account result: $done")

                refreshAccount()
            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)
                info(e)
                handleApiError(e)
            } catch (e: Exception) {
                showProgress(false)
                allowInput(true)
                info(e)
                handleException(e)
            }
        }
    }

    private suspend fun refreshAccount() {
        val account = withContext(Dispatchers.IO) {
            sessionManager.loadAccount()?.refresh()
        } ?: return

        info("Account after bound: $account")

        toast(R.string.prompt_account_updated)

        sessionManager.saveAccount(account)

        showProgress(false)

        setResult(Activity.RESULT_OK)

        finish()
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun startForResult(activity: Activity?, account: Account) {
            val intent = Intent(activity, LinkActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, account)
            }

            activity?.startActivityForResult(intent, RequestCode.LINK)
        }
    }
}
