package com.ft.ftchinese.ui.account

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
import com.ft.ftchinese.model.order.Cycle
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.Tier
import com.ft.ftchinese.model.order.subsPlans
import com.ft.ftchinese.ui.pay.*
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.util.formatLocalDate
import kotlinx.android.synthetic.main.activity_member.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class MemberActivity : ScopedAppActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
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

        toast(R.string.prompt_refreshing)

        accountViewModel.refresh(account)
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

        accountViewModel.accountRefreshed.observe(this, Observer {
            stopRefresh()

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

            initUI()
        })

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
                SubscriptionActivity.start(this, subsPlans.of(Tier.PREMIUM, Cycle.YEAR))
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
