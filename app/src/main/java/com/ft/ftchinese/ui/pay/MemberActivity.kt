package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMemberBinding
import com.ft.ftchinese.model.order.StripeSubResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.subscription.*
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.tracking.PaywallTracker
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.paywall.CustomerServiceFragment
import com.ft.ftchinese.ui.paywall.PaywallActivity
import com.ft.ftchinese.ui.paywall.UpgradeActivity
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class MemberActivity : ScopedAppActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: ActivityMemberBinding

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
        accountViewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)


        accountViewModel.stripeSubsRefreshed.observe(this) {
            onStripeSubRefreshed(it)
        }

        accountViewModel.iapRefreshResult.observe(this) {
            onIAPRefresh(it)
        }

        accountViewModel.accountRefreshed.observe(this) {
            onAccountRefreshed(it)
        }

        initUI()

        supportFragmentManager.commit {
            replace(R.id.frag_customer_service, CustomerServiceFragment.newInstance())
        }
    }

    private fun initUI() {

        val account = sessionManager.loadAccount() ?: return

        val member = account.membership

        info(member)

        binding.member = buildMemberStatus(this, member)

        info(buildMemberStatus(this, member))

        binding.subscribeBtn.setOnClickListener {
            PaywallActivity.start(this)
            it.isEnabled = false
        }

        binding.renewBtn.setOnClickListener {

            val plan = member.getPlan() ?: return@setOnClickListener
            // Tracking
            PaywallTracker.fromRenew()

            CheckOutActivity.startForResult(
                    activity = this,
                    requestCode = RequestCode.PAYMENT,
                    checkout = FtcCheckout(
                        kind = OrderKind.RENEW,
                        plan = plan
                    )
            )

            it.isEnabled = false
        }

        binding.upgradeBtn.setOnClickListener {
            if (member.isAliOrWxPay()) {

                UpgradeActivity.startForResult(this, RequestCode.PAYMENT)

            } else if (member.payMethod == PayMethod.STRIPE) {
                val price = StripePriceStore
                    .find(Tier.PREMIUM, Cycle.YEAR)
                    ?: return@setOnClickListener

                StripeSubActivity.startForResult(
                    activity = this,
                    requestCode = RequestCode.PAYMENT,
                    price = price
                )
            }

            it.isEnabled = false
        }
    }

    /**
     * Refresh account.
     * Use different API endpoints depending on the login method.
     */
    override fun onRefresh() {
        if (!isConnected) {
            stopRefresh()

            toast(R.string.prompt_no_network)
            return
        }

        val account = sessionManager.loadAccount()

        if (account == null) {
            stopRefresh()
            toast("Your account data not found!")
            return
        }

        when (account.membership.payMethod) {
            PayMethod.ALIPAY, PayMethod.WXPAY, null -> {
                toast(R.string.refreshing_account)
                accountViewModel.refresh(account)
            }
            PayMethod.STRIPE -> {
                toast(R.string.refreshing_stripe_sub)
                accountViewModel.refreshStripeSub(account)
            }
            PayMethod.APPLE -> {
                toast(R.string.refresh_iap_sub)
                accountViewModel.refreshIAPSub(account)
            }
        }
    }

    private fun onStripeSubRefreshed(result: Result<StripeSubResult>) {
        stopRefresh()

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast("Stripe subscription refreshed!")
                sessionManager.saveMembership(result.data.membership)
            }
        }
    }

    private fun onIAPRefresh(result: Result<IAPSubs>) {
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast("Apple IAP subscription updated!")
            }
        }

        val account = sessionManager.loadAccount()
        if (account == null) {
            stopRefresh()
            return
        }

        toast(R.string.refreshing_account)
        accountViewModel.refresh(account)
    }

    private fun onAccountRefreshed(accountResult: Result<Account>) {
        stopRefresh()

        when (accountResult) {
            is Result.LocalizedError -> {
                toast(accountResult.msgId)
            }
            is Result.Error -> {
                accountResult.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_updated)

                sessionManager.saveAccount(accountResult.data)

                initUI()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.subscribeBtn.isEnabled = true
        binding.renewBtn.isEnabled = true
        binding.upgradeBtn.isEnabled = true

        initUI()
    }


    /**
     * After [CheckOutActivity] finished, it sends activity result here.
     * This activity kills itself since the [CheckOutActivity]
     * will display a new [MemberActivity] to show updated
     * membership.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        info("onActivityResult requestCode: $requestCode, resultCode: $resultCode")

        when (requestCode) {
            RequestCode.PAYMENT -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }

                finish()
            }
        }
    }

    // Create menus
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.activity_member_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_orders -> {
            MyOrdersActivity.start(this)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(Intent(context, MemberActivity::class.java))
        }
    }
}
