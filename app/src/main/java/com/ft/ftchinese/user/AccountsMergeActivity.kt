package com.ft.ftchinese.user

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.activity_accounts_merge.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

/**
 * Show details of account to be bound, show a button to let
 * user to confirm the performance, or just deny accounts merging.
 */
class AccountsMergeActivity : AppCompatActivity(), AnkoLogger {

    private var job: Job? = null
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

        val data = intent.getStringExtra(EXTRA_ACCOUNT)

        otherAccount = json.parse<Account>(data)


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

        val ftcMemberFrag = MembershipFragment.newInstance(
                m = ftcAccount.membership,
                heading = "FT中文网账号\n${ftcAccount.email}"
        )
        val wxMemberFrag = MembershipFragment.newInstance(
                m = wxAccount.membership,
                heading = "微信账号\n${wxAccount.wechat.nickname}"
        )

        supportFragmentManager.beginTransaction()
                .replace(R.id.frag_container_1, ftcMemberFrag)
                .replace(R.id.frag_container_2, wxMemberFrag)
                .commit()


        // If the two accounts are already bound.
        if (ftcAccount.isEqual(wxAccount)) {
            result_tv.text = """
                两个账号已经绑定，无需操作。如果您未看到绑定后的账号信息，请返回"账号与安全"后下拉刷新
            """.trimIndent()
            return
        }

        // If FTC account is already bound to another wechat.
        if (ftcAccount.isCoupled) {
            result_tv.text = "账号${ftcAccount.email}已经绑定了其他微信账号，只能绑定一个微信账号"
            return
        }

        if (wxAccount.isCoupled) {
            result_tv.text = "微信账号${wxAccount.wechat.nickname}已经绑定了其他FT中文网账号，只能绑定一个FT中文网账号"
            return
        }

        // Both accounts have memberships and not expired yet.
        // Tell user that
        if (!ftcAccount.membership.isExpired && !wxAccount.membership.isExpired) {
            result_tv.text = "微信账号和FT中文网的账号均购买了会员服务，两个会员均未到期。合并账号会删除其中一个账号的会员信息。为保障您的权益，暂不支持绑定两个会员未过期的账号。您可以寻取客服帮助。"
            return
        }

        start_binding_btn.setOnClickListener {
            bind(ftcAccount.id, wxAccount.unionId)
        }
    }

    private fun bind(userId: String, unionId: String?) {
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

        job = GlobalScope.launch(Dispatchers.Main) {
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

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        private const val EXTRA_ACCOUNT = "extra_account"

        fun startForResult(activity: Activity?, account: Account) {
            val intent = Intent(activity, AccountsMergeActivity::class.java).apply {
                putExtra(EXTRA_ACCOUNT, json.toJsonString(account))
            }

            activity?.startActivityForResult(intent, RequestCode.BOUND)
        }
    }
}