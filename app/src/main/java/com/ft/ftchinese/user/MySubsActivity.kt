package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.base.getMemberTypeText
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.activity_my_subscription.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class MySubsActivity : AppCompatActivity(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var job: Job? = null

    private lateinit var viewAdapter: RowAdapter
    private lateinit var sessionManager: SessionManager

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
    }

    private fun showRenewal(value: Boolean) {
        if (value) {
            renew_btn.visibility = View.VISIBLE
        } else {
            renew_btn.visibility = View.GONE
        }
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

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val refreshedAccount = withContext(Dispatchers.IO) {
                    account.refresh()
                }

                stopRefresh()

                info("Refreshed account: $refreshedAccount")

                if (refreshedAccount == null) {
                    toast("Account not retrieved")
                    return@launch
                }

                toast(R.string.prompt_updated)

                sessionManager.saveAccount(refreshedAccount)

                updateUI(refreshedAccount)

            } catch (e: ClientError) {
                info("$e")

                stopRefresh()

                when (e.statusCode) {
                    // Logout this user if account not found during refresh.
                    404 -> {
                        toast(R.string.api_account_not_found)
                        sessionManager.logout()
                    }
                    else -> handleApiError(e)
                }

            } catch (e: Exception) {
                stopRefresh()

                handleException(e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_subscription)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        swipe_refresh.setOnRefreshListener(this)

        sessionManager = SessionManager.getInstance(this)

        initUI()

        order_list_btn.setOnClickListener {
            MyOrdersActivity.start(this)
        }
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return

        viewAdapter = RowAdapter(buildRows(account))

        member_rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MySubsActivity)
            adapter = viewAdapter
        }

        supportFragmentManager.beginTransaction()
                .replace(R.id.frag_customer_service, CustomerServiceFragment.newInstance())
                .commit()


        updateUIRenewal(account.membership)
    }

    private fun updateUIRenewal(membership: Membership) {

        if (!membership.isRenewable) {
            showRenewal(false)
            return
        }

        showRenewal(true)

        val tier = membership.tier ?: return
        val cycle = membership.cycle ?: return

        renew_btn.setOnClickListener {

            PaywallTracker.source = ChannelItem(
                    id = "RenewButton",
                    type = "MySubscription",
                    title = ""
            )

            RenewalActivity.startForResult(
                    activity = this,
                    requestCode = RequestCode.PAYMENT,
                    tier = tier,
                    cycle = cycle)
        }
    }

    private fun updateUI(account: Account) {
        val rows = buildRows(account)

        viewAdapter.refreshData(rows)
        viewAdapter.notifyDataSetChanged()

        updateUIRenewal(account.membership)
    }

    private fun buildRows(account: Account): Array<TableRow> {

        val tierText = if (account.isVip) {
            getString(R.string.tier_vip)
        } else {
            getMemberTypeText(account.membership)
        }

        val endDateText = if (account.isVip) {
            getString(R.string.cycle_vip)
        } else {
            formatLocalDate(account.membership.expireDate)
        }

        val row1 = TableRow(
                header = getString(R.string.label_member_tier),
                data = tierText,
                isBold = true
        )

        val row2 = TableRow(
                header = getString(R.string.label_member_duration),

                data = endDateText ?: "",
                color = ContextCompat.getColor(this, R.color.colorClaret)
        )

        return arrayOf(row1, row2)
    }

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

    companion object {
        fun start(context: Context?) {
            context?.startActivity(Intent(context, MySubsActivity::class.java))
        }
    }
}

