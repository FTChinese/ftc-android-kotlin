package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAccountBinding
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.customer.CustomerViewModel
import com.ft.ftchinese.ui.customer.CustomerViewModelFactory
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.viewmodel.WxRefreshState
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
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: ActivityAccountBinding

    private lateinit var customerViewModel: CustomerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        accountViewModel = ViewModelProvider(this)
            .get(AccountViewModel::class.java)

        customerViewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(FileCache(this)),
        ).get(CustomerViewModel::class.java)

        connectionLiveData.observe(this) {
            accountViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            accountViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        accountViewModel.progressLiveData.observe(this, {
            binding.inProgress = it
        })

        customerViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        accountViewModel.uiType.observe(this, {
            initUI()
        })

        // Launch wechat authorization if access token
        // expired.
        accountViewModel.wxRefreshResult.observe(this, Observer {
            if (it !is FetchResult.Success) {
                return@Observer
            }
            if (it.data == WxRefreshState.ReAuth) {

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
                replace(R.id.frag_account, FtcAccountFragment.newInstance())
            }
        }
    }

    /**
     * Receive results from
     * [UpdateActivity] or [LinkFtcActivity].
     *
     * Source and their meanings:
     *
     * [LinkFtcActivity] - Wechat user link to FTC by either
     * sign up or login. Request code is RequestCode.LINK.
     * If email alrady exists, the result is relayed from
     * [LinkPreviewActivity].
     *
     * [WxInfoActivity] - Unlink wechat.
     * The event is originated from [UnlinkActivity].
     *
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
