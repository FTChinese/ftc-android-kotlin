package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityLinkPreviewBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.LinkViewModel
import com.ft.ftchinese.viewmodel.Result
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
    private lateinit var binding: ActivityLinkPreviewBinding

    private var otherAccount: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_link_preview)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        otherAccount = intent.getParcelableExtra(EXTRA_ACCOUNT)

        linkViewModel = ViewModelProvider(this)
                .get(LinkViewModel::class.java)

        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        initUI()

        linkViewModel.linkResult.observe(this, Observer {
            onLinkResult(it)
        })

        // After link finished, retrieve new account data from API.
        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
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

    // Check whether two account are allowed to link.
    // Return a string describing the reason of denial if not permitted,
    // or null if permitted.
    private fun permitLinking(ftcAccount: Account, wxAccount: Account): String? {
        // If the two accounts are already bound.
        if (ftcAccount.isEqual(wxAccount)) {
            return getString(R.string.accounts_already_linked)
        }

        // If FTC account is already bound to another wechat.
        if (ftcAccount.isLinked) {
            return getString(R.string.ftc_account_linked, ftcAccount.email)
        }

        if (wxAccount.isLinked) {
           return getString(R.string.wx_account_linked, wxAccount.wechat.nickname)
        }

        // Both accounts have memberships and not expired yet.
        if (!ftcAccount.membership.expired() && !wxAccount.membership.expired()) {
            return getString(R.string.accounts_member_valid)
        }

        return null
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

        val denialReason = permitLinking(ftcAccount, wxAccount)

        if (denialReason != null) {
            binding.resultTv.text = denialReason
            binding.btnStartLink.isEnabled = false
            return
        }

        val unionId = wxAccount.unionId ?: return

        binding.btnStartLink.setOnClickListener {
            if (!isConnected) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            binding.inProgress = true
            it.isEnabled = false

            linkViewModel.link(ftcAccount.id, unionId)
        }
    }



    private fun onLinkResult(result: Result<Boolean>) {

        when (result) {
            is Result.LocalizedError -> {
                binding.inProgress = false
                toast(result.msgId)
            }
            is Result.Error -> {
                binding.inProgress = false
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_linked)

                if (!isConnected) {
                    binding.inProgress = false
                    finish()
                    return
                }

                // Silently refresh account since this is not
                // required.
                val account = sessionManager.loadAccount()

                if (account == null) {
                    binding.inProgress = false
                    finish()
                    return
                }

                toast(R.string.refreshing_account)
                accountViewModel.refresh(account)
            }
        }
    }

    private fun onAccountRefreshed(accountResult: Result<Account>) {
        binding.inProgress = false

        when (accountResult) {
            is Result.LocalizedError -> {
                toast(accountResult.msgId)
            }
            is Result.Error -> {
                accountResult.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                sessionManager.saveAccount(accountResult.data)
            }
        }

        /**
         * Pass data back to [LinkFtcActivity].
         * If this activity is started from WxEntryActivity,
         * it is meaningless to pass data back.
         */
        setResult(Activity.RESULT_OK)
        finish()
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
