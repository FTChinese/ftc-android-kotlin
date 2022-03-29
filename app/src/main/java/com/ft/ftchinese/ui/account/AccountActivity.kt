package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityAccountBinding
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.customer.CustomerViewModel
import com.ft.ftchinese.ui.customer.CustomerViewModelFactory
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.dialog.DialogArgs
import com.ft.ftchinese.ui.login.AuthActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel

/**
 * Show user's account details.
 * Show different fragments based on whether FTC account is bound to wechat account.
 * If user logged in with email account, show FtcAccountFragment;
 * If user logged in with wechat account and it is not bound to an FTC account, show WxAccountFragment;
 * If user logged in with wechat account and it is bound to an FTC account, show FtcAccountFragment.
 */
class AccountActivity : ScopedAppActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: ActivityAccountBinding

    private lateinit var customerViewModel: CustomerViewModel
    private lateinit var wxInfoViewModel: WxInfoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_account)
        setSupportActionBar(binding.toolbar.toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        accountViewModel = ViewModelProvider(this)[AccountViewModel::class.java]

        customerViewModel = ViewModelProvider(
            this,
            CustomerViewModelFactory(FileCache(this)),
        )[CustomerViewModel::class.java]

        wxInfoViewModel = ViewModelProvider(this)[WxInfoViewModel::class.java]

        connectionLiveData.observe(this) {
            accountViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
            wxInfoViewModel.isNetworkAvailable.value = it
        }

        isConnected.let {
            accountViewModel.isNetworkAvailable.value = it
            customerViewModel.isNetworkAvailable.value = it
            wxInfoViewModel.isNetworkAvailable.value = it
        }

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        accountViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        customerViewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        accountViewModel.uiSwitched.observe(this) {
            initUI()
        }
    }

    private fun initUI() {
        val account = sessionManager.loadAccount()
        // If the account is deleted
        if (account == null) {
            AuthActivity.start(this)
            finish()
            return
        }

        supportFragmentManager.commit {
            if (account.isWxOnly) {
                replace(R.id.frag_account, WxInfoFragment.newInstance())
            } else {
                replace(R.id.frag_account, FtcAccountFragment.newInstance())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val a = sessionManager.loadAccount()

        if (a?.loginMethod != LoginMethod.WECHAT) {
            menuInflater.inflate(R.menu.activity_account_menu, menu)
            return true
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_account -> {
                alertConfirmDelete()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun alertConfirmDelete() {
        AlertDialogFragment.newInstance(
            params = DialogArgs(
                title = R.string.title_confirm_delete_account,
                message = getString(R.string.message_warn_delete_account),
                positiveButton = R.string.button_delete_account,
                negativeButton = R.string.button_think_twice,
            )
        )
            .onPositiveButtonClicked { dialog, _ ->
                dialog.dismiss()
                UpdateActivity.start(this, AccountRowType.DELETE)
            }
            .onNegativeButtonClicked { dialog, _ ->
                dialog.cancel()
            }
            .show(supportFragmentManager, "ConfirmDeleteAccount")
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

        // UNLINK is Passsed back from WxInfoActivity;
        // LINK is passed from WXEntryActivity.
        // Used when user is logged in using email account.
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
