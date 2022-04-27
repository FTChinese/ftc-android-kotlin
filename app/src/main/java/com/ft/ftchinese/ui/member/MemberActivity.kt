package com.ft.ftchinese.ui.member

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMemberBinding
import com.ft.ftchinese.model.enums.PayMethod
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.iapsubs.IAPSubsResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.Membership
import com.ft.ftchinese.model.stripesubs.StripeSubsResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.SubsActivity
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import org.jetbrains.anko.alert
import org.jetbrains.anko.appcompat.v7.Appcompat
import org.jetbrains.anko.toast

class MemberActivity : ScopedAppActivity(),
        SwipeRefreshLayout.OnRefreshListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var subsStatusViewModel: SubsStatusViewModel
    private lateinit var binding: ActivityMemberBinding
    private var addonFragment: SubsAddOnFragment? = null

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    private fun stopRefresh() {
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_member)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        binding.swipeRefresh.setOnRefreshListener(this)
        sessionManager = SessionManager.getInstance(this)

        setupViewModel()
        initUI()

        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun setupViewModel() {
        accountViewModel = ViewModelProvider(this)[AccountViewModel::class.java]

        subsStatusViewModel = ViewModelProvider(this)[SubsStatusViewModel::class.java]

        connectionLiveData.observe(this) {
            accountViewModel.isNetworkAvailable.value = it
        }
        isConnected.let {
            accountViewModel.isNetworkAvailable.value = it
        }

        subsStatusViewModel.reactivateStripeRequired.observe(this) {
            if (!it) {
                return@observe
            }

            binding.inProgress = true
            toast(R.string.stripe_refreshing)
            sessionManager.loadAccount()?.let {
                accountViewModel.reactivateStripe(it)
            }
        }

        // For one-time purchase refreshing, we will simply
        // retrieve user account.
        accountViewModel.accountRefreshed.observe(this) { result: FetchResult<Account> ->
            stopRefresh()

            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.TextError -> toast(result.text)
                is FetchResult.Success -> {
                    toast(R.string.refresh_success)
                    sessionManager.saveAccount(result.data)
                    subsChanged(result.data.membership)
                    setResult(Activity.RESULT_OK)
                }
            }
        }

        accountViewModel.addOnResult.observe(this) { result: FetchResult<Membership> ->
            stopRefresh()

            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.TextError -> toast(result.text)
                is FetchResult.Success -> {
                    toast(R.string.refresh_success)
                    sessionManager.saveMembership(result.data)
                    subsChanged(result.data)
                    setResult(Activity.RESULT_OK)
                }
            }
        }

        // For Apple subscription, we will verify user's existing
        // receipt against App Store.
        accountViewModel.iapRefreshResult.observe(this) { result: FetchResult<IAPSubsResult> ->
            when (result) {
                is FetchResult.LocalizedError -> toast(result.msgId)
                is FetchResult.TextError -> toast(result.text)
                is FetchResult.Success -> {
                    sessionManager.saveMembership(result.data.membership)
                    toast(R.string.iap_refresh_success)
                    subsChanged(result.data.membership)
                    setResult(Activity.RESULT_OK)
                }
            }
        }

        // Result of refreshing Stripe, or canceling/reactivating subscription.
        accountViewModel.stripeResult.observe(this) { result: FetchResult<StripeSubsResult> ->
            stopRefresh()
            binding.inProgress = false

            when (result) {
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                }
                is FetchResult.TextError -> {
                    toast(result.text)
                }
                is FetchResult.Success -> {
                    toast(R.string.stripe_refresh_success)
                    sessionManager.saveMembership(result.data.membership)
                    subsChanged(result.data.membership)
                    setResult(Activity.RESULT_OK)
                }
            }
        }
    }

    private fun initUI() {

        supportFragmentManager.commit {
            replace(R.id.frag_subs_status, MembershipFragment.newInstance())
            replace(R.id.frag_subs_rule, SubsRuleFragment.newInstance())
            replace(R.id.frag_customer_service, CustomerServiceFragment.newInstance())
        }

        binding.subsUpdate.setOnClickListener {
            resultLauncher.launch(SubsActivity.intent(this))
        }

        sessionManager.loadAccount()?.membership?.let {
            subsChanged(it)
        }
    }

    // Update membership status ui.
    private fun subsChanged(m: Membership) {

        if (m.hasAddOn) {
            SubsAddOnFragment.newInstance().let {
                supportFragmentManager.commit {
                    replace(R.id.frag_subs_addon, it)
                }
                addonFragment = it
            }
        } else {
            addonFragment?.let {
                supportFragmentManager.commit {
                    remove(it)
                }
            }
        }

        val status = SubsStatus.newInstance(
            this,
            m
        )

        subsStatusViewModel.statusChanged.value = status

        invalidateOptionsMenu()
    }

    /**
     * Refresh account.
     * Use different API endpoints depending on the login method.
     */
    override fun onRefresh() {
        val account = sessionManager.loadAccount(raw = true)

        if (account == null) {
            stopRefresh()
            toast("Your account data not found!")
            return
        }

        if (account.membership.autoRenewOffExpired && account.membership.hasAddOn) {
            toast(R.string.refreshing_account)
            accountViewModel.migrateAddOn(account)
            return
        }

        when (account.membership.payMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY, null -> {
                toast(R.string.refreshing_account)
                accountViewModel.refresh(account)
            }
            PayMethod.STRIPE -> {
                toast(R.string.stripe_refreshing)
                accountViewModel.refreshStripe(account)
            }
            PayMethod.APPLE -> {
                toast(R.string.iap_refreshing)
                accountViewModel.refreshIAP(account)
            }
            else -> toast("Current payment method unknown!")
        }
    }

    override fun onResume() {
        super.onResume()
        initUI()
    }

    // Create menus
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val m = sessionManager.loadAccount()?.membership

        menuInflater.inflate(R.menu.activity_member_menu, menu)
        menu.findItem(R.id.action_cancel_stripe)?.isVisible = m?.canCancelStripe ?: false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.action_cancel_stripe -> {

            alert(Appcompat, "该操作将关闭Stripe自动续订，当前订阅在到期前依然有效。在订阅到期前，您随时可以重新打开自动续订。", "取消订阅") {
                positiveButton("确认关闭") {
                    binding.inProgress = true
                    sessionManager.loadAccount()?.let {
                        accountViewModel.cancelStripe(it)
                    }
                }
                negativeButton("再考虑一下") {
                    it.dismiss()
                }
            }.show()

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        setResult(Activity.RESULT_OK)
    }

    companion object {

        fun start(context: Context?) {
            context?.startActivity(Intent(context, MemberActivity::class.java))
        }

        fun startForResult(activity: Activity?) {
            activity?.startActivityForResult(
                Intent(activity, MemberActivity::class.java),
                RequestCode.MEMBER_REFRESHED
            )
        }
    }
}
