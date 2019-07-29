package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.model.order.*
import com.ft.ftchinese.ui.account.AccountViewModel
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.util.formatLocalDate
import kotlinx.android.synthetic.main.activity_member.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

private val subStatusText = mapOf(
    StripeSubStatus.Active to R.string.sub_status_active,
        StripeSubStatus.Incomplete to R.string.sub_status_incomplete,
        StripeSubStatus.IncompleteExpired to R.string.sub_status_incomplete_expired,
        StripeSubStatus.Trialing to R.string.sub_status_trialing,
        StripeSubStatus.PastDue to R.string.sub_status_past_due,
        StripeSubStatus.Canceled to R.string.sub_status_cancled,
        StripeSubStatus.Unpaid to R.string.sub_status_unpaid
)

@kotlinx.coroutines.ExperimentalCoroutinesApi
class MemberActivity : ScopedAppActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var checkOutViewModel: CheckOutViewModel

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_member)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        swipe_refresh.setOnRefreshListener(this)

        sessionManager = SessionManager.getInstance(this)
        accountViewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        checkOutViewModel = ViewModelProviders.of(this)
                .get(CheckOutViewModel::class.java)

        checkOutViewModel.stripeSubResult.observe(this, Observer {
                onStripeSubRefreshed(it)
        })

        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        initUI()
    }

    /**
     * Refresh account.
     * Use different API endpoints depending on the login method.
     */
    override fun onRefresh() {
        if (!isNetworkConnected()) {
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

        // TODO: what if user ended stripe pay, and change
        // to other payment methods like ali, or wechat?
        // In such cases, this app has no way to know the
        // the changes. And if we start refresh,  API
        // will continue to fetch user's stripe subscription,
        // and the invalid stipe data will override
        // user's later changed valid data!
        // What's more, even this step errored, we still
        // need to proceed with account refreshing!
        if (account.membership.payMethod == PayMethod.STRIPE) {
            toast(R.string.refreshing_stripe_sub)

            checkOutViewModel.refreshStripeSub(account)

            return
        }

        startRefreshingAccount(account)
    }

    private fun startRefreshingAccount(account: Account) {
        toast(R.string.refreshing_account)
        accountViewModel.refresh(account)
    }

    private fun onStripeSubRefreshed(subResult: StripeSubResult?) {
        val account = sessionManager.loadAccount()
        if (account == null) {
            stopRefresh()
            return
        }

        if (subResult == null) {
            toast(R.string.stripe_refreshing_failed)
            startRefreshingAccount(account)
            return
        }

        if (subResult.error != null) {
            toast(subResult.error)
            startRefreshingAccount(account)
            return
        }

        if (subResult.exception != null) {
            handleException(subResult.exception)
            startRefreshingAccount(account)
            return
        }

        // Even if user's subscription data is not refresh
        // (for example, API responded 404, in which case
        // subResult.success is null), account still
        // needs to be refreshed.
        // So here we do not perform checks on subResult == null.
        // It is just an indicator that network finished,
        // regardless of result.
        startRefreshingAccount(account)
    }

    private fun onAccountRefreshed(accountResult: AccountResult?) {
        stopRefresh()

        if (accountResult == null) {
            return
        }

        if (accountResult.error != null) {
            toast(accountResult.error)
            return
        }

        if (accountResult.exception != null) {
            handleException(accountResult.exception)
            return
        }

        if (accountResult.success == null) {
            toast("Unknown error")
            return
        }

        toast(R.string.prompt_updated)

        sessionManager.saveAccount(accountResult.success)

        initUI()
    }

    override fun onResume() {
        super.onResume()
        resubscribe_btn.isEnabled = true
        renew_btn.isEnabled = true
        upgrade_btn.isEnabled = true

        initUI()
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        tv_member_tier.text = when (account.membership.tier) {
            Tier.STANDARD -> getString(R.string.tier_standard)
            Tier.PREMIUM -> getString(R.string.tier_premium)
            else -> if (account.isVip) getString(R.string.tier_vip) else getString(R.string.tier_free)

        }

        tv_expire_date.text = if (account.isVip) {
            getString(R.string.cycle_vip)
        } else {
            formatLocalDate(account.membership.expireDate)
        }

        if (account.membership.payMethod == PayMethod.STRIPE) {
            val statusId = subStatusText[account.membership.status]
            val statusStr = if (statusId == null) account.membership.status.toString() else getString(statusId)

            tv_sub_status.text = getString(
                    R.string.label_stripe_status,
                    statusStr)
        } else {
            tv_sub_status.visibility = View.GONE
        }

        // Show auto renewal status to all members.
        val autoRenewalId = if (account.membership.autoRenew == true) R.string.yes else R.string.no
        tv_auto_renewal.text = getString(
                R.string.auto_renewal,
                getString(autoRenewalId)
        )

        hideButton()
        setupButtons(account.membership)
    }

    private fun setupButtons(member: Membership) {
        // re-subscription button always performs the same
        // action regardless of user's membership status.
        resubscribe_btn.setOnClickListener {
            PaywallActivity.start(this)
            it.isEnabled = false
        }

        if (member.fromWxOrAli()) {

            if (member.isExpired) {
                resubscribe_btn.visibility = View.VISIBLE
                return
            }

            // If a member in standard tier, always permit
            // switching to premium.
            if (member.tier == Tier.STANDARD) {
                upgrade_btn.visibility = View.VISIBLE
                upgrade_btn.setOnClickListener {
                    UpgradeActivity.startForResult(this, RequestCode.PAYMENT)
                    it.isEnabled = false
                }
            }

            // User is only allowed to renew as long
            // as membership expire date is within
            // allowed range.
            if (!member.canRenew()) {
                return
            }

            renew_btn.visibility = View.VISIBLE
            renew_btn.setOnClickListener {

                val plan = member.getPlan() ?: return@setOnClickListener
                // Tracking
                PaywallTracker.fromRenew()

                CheckOutActivity.startForResult(
                        activity = this,
                        requestCode = RequestCode.PAYMENT,
                        plan = plan
                )

                it.isEnabled = false
            }
        }

        if (member.payMethod == PayMethod.STRIPE) {
            if (member.isExpired) {
                if (member.autoRenew == true) {
                    // TODO: data is stale. Refresh.
                    return
                }
                resubscribe_btn.visibility = View.VISIBLE
            }
            if (member.tier == Tier.PREMIUM) {
                return
            }

            upgrade_btn.visibility = View.VISIBLE
            // Switch plan.
            upgrade_btn.setOnClickListener {
                StripeSubActivity.startForResult(
                        this,
                        RequestCode.PAYMENT,
                        subsPlans.of(
                                Tier.PREMIUM,
                                Cycle.YEAR
                        )
                )
                it.isEnabled = false
            }
        }
    }

    // Hide all buttons on create.
    private fun hideButton() {
        resubscribe_btn.visibility = View.GONE
        renew_btn.visibility = View.GONE
        upgrade_btn.visibility = View.GONE
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

//                finish()
            }
        }
    }

    // Create menus
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.activity_member_menu, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_orders -> {
            MyOrdersActivity.start(this)
            true
        }
        R.id.action_service -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, "subscriber.service@ftchinese.com")
                putExtra(Intent.EXTRA_SUBJECT, "FT中文网会员订阅")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                toast(R.string.prompt_no_email_app)
            }

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
