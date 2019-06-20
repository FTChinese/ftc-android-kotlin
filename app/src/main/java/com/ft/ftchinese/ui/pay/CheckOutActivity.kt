package com.ft.ftchinese.ui.pay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.alipay.sdk.app.PayTask
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.*
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.RowAdapter
import com.ft.ftchinese.ui.TableRow
import com.ft.ftchinese.ui.account.MemberActivity
import com.ft.ftchinese.util.ClientError
import com.tencent.mm.opensdk.constants.Build
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_check_out.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class CheckOutActivity : ScopedAppActivity(),
        AnkoLogger {

    private lateinit var checkOutViewModel: CheckOutViewModel

    private lateinit var orderManager: OrderManager
    private lateinit var sessionManager: SessionManager
    private lateinit var wxApi: IWXAPI
    private lateinit var tracker: StatsTracker

    private var plan: PlanPayable? = null

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_out)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val p = intent.getParcelableExtra<PlanPayable>(EXTRA_PLAN_PAYABLE) ?: return

        initUI(p)

        this.plan = p

        checkOutViewModel = ViewModelProviders.of(this).get(CheckOutViewModel::class.java)

        // Observing pay button.
        checkOutViewModel.payStarted.observe(this, Observer<PayMethod> {
            when (it) {
                PayMethod.ALIPAY -> aliPay()
                PayMethod.WXPAY -> wxPay()
                PayMethod.STRIPE -> stripePay()
                else -> toast(R.string.pay_method_unknown)
            }
        })

        checkOutViewModel.directUpgrade.observe(this, Observer<Boolean> {
            if (!it) {
                return@Observer
            }

            // Start upgrading without payment.
            directUpgrade()
        })

        sessionManager = SessionManager.getInstance(this)
        orderManager = OrderManager.getInstance(this)

        wxApi = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)
        wxApi.registerApp(BuildConfig.WX_SUBS_APPID)
        tracker = StatsTracker.getInstance(this)

        // Log event: add card
        tracker.addCart(p)
    }

    private fun initUI(p: PlanPayable) {

        upgrade_smallprint.visibility = View.GONE

        // For upgrading show some additional information.
         if (p.isUpgrade) {
             supportActionBar?.setTitle(R.string.title_upgrade)

             if (!p.isDirectUpgrade()) {
                 upgrade_smallprint.visibility = View.VISIBLE
                 upgrade_smallprint.text = getString(R.string.upgrade_smallprint, p.cycleCount, p.extraDays)
             }

         } else {
             upgrade_smallprint.visibility = View.GONE

             if (p.isRenew) {
                 supportActionBar?.setTitle(R.string.title_renewal)
             }
         }

        product_rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@CheckOutActivity)
            adapter = RowAdapter(buildRows(p))
        }

        supportFragmentManager.commit {
            if (!p.isPayRequired()) {
                replace(R.id.frag_confirm, DirectUpgradeFragment.newInstance())
            } else {
                replace(R.id.frag_pay_method, PayMethodFragment.newInstance())
                replace(R.id.frag_confirm, PayFragment.newInstance(p.payable))
            }
        }
    }

    private fun buildRows(p: PlanPayable): Array<TableRow> {
        if (!p.isUpgrade) {

            return arrayOf(
                    TableRow(
                            header = getString(R.string.label_member_tier),
                            data = getTierCycleText(p.tier, p.cycle) ?: "",
                            isBold = true
                    ),
                    TableRow(
                            header = getString(R.string.label_price),
                            data = getString(R.string.formatter_price, p.payable),
                            color = try {
                                ContextCompat.getColor(this, R.color.colorClaret)
                            } catch (e: Exception) {
                                null
                            }
                    )
            )
        }
        val row1 = TableRow(
                header = getString(R.string.label_member_tier),
                data = getTierCycleText(p.tier, p.cycle) ?: "",
                isBold = true
        )

        val row2 = TableRow(
                header = "会员定价",
                data = getString(R.string.formatter_price, p.netPrice)
        )

        val row3 = TableRow(
                header = "当前余额",
                data = getString(R.string.formatter_price, p.balance)
        )

        val row4 = TableRow(
                header = "应付金额",
                data = getString(R.string.formatter_price, p.payable),
                color = try {
                    ContextCompat.getColor(this, R.color.colorClaret)
                } catch (e: Exception) {
                    null
                }
        )

        return arrayOf(row1, row2, row3, row4)
    }

    private fun wxPay() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        // Check wechat version to make sure it suports payment.
        val supportedApi = wxApi.wxAppSupportAPI
        if (supportedApi < Build.PAY_SUPPORTED_SDK_INT) {

            toast(R.string.wxpay_not_supported)
            return
        }

        val tier = plan?.tier ?: return
        val cycle = plan?.cycle ?: return
        val account = sessionManager.loadAccount() ?: return

        tracker.checkOut(plan?.payable ?: 0.0, PayMethod.WXPAY)

        showProgress(true)

        launch {
            try {
                val wxOrder = withContext(Dispatchers.IO) {
                    account.wxPlaceOrder(tier, cycle)
                }

                showProgress(false)

                if (wxOrder == null) {
                    toast(R.string.order_cannot_be_created)

                    checkOutViewModel.enableInput(true)

                    tracker.buyFail(tier)

                    return@launch
                }

                info("Prepay order: ${wxOrder.ftcOrderId}, ${wxOrder.prepayId}")

                val req = PayReq()
                req.appId = wxOrder.appId
                req.partnerId = wxOrder.partnerId
                req.prepayId = wxOrder.prepayId
                req.nonceStr = wxOrder.nonce
                req.timeStamp = wxOrder.timestamp
                req.packageValue = wxOrder.pkg
                req.sign = wxOrder.signature

                wxApi.registerApp(req.appId)
                val result = wxApi.sendReq(req)

                info("Call sendReq result: $result")

                // Save order details
                if (result) {
                    val subs = Subscription(
                            orderId = wxOrder.ftcOrderId,
                            tier = tier,
                            cycle = cycle,
                            cycleCount = plan?.cycleCount ?: 1,
                            extraDays = plan?.extraDays ?: 0,
                            netPrice = wxOrder.netPrice,
                            payMethod = PayMethod.WXPAY,
                            isUpgrade = plan?.isUpgrade ?: false
                    )

                    // Save subscription details to shared preference so that we could use it in WXPayEntryActivity
                    orderManager.save(subs)
                }

                wxpayStarted()

            } catch (e: ClientError) {
                info(e)

                showProgress(false)
                checkOutViewModel.enableInput(true)
                handleClientError(e)

                tracker.buyFail(tier)

            } catch (e: Exception) {
                info(e)

                showProgress(false)
                checkOutViewModel.enableInput(true)
                handleException(e)

                tracker.buyFail(tier)
            }
        }
    }

    // Destroy self and tell calling activity result
    private fun wxpayStarted() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun aliPay() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            return
        }

        val tier = plan?.tier ?: return
        val cycle = plan?.cycle ?: return
        val account = sessionManager.loadAccount() ?: return

        showProgress(true)

        tracker.checkOut(plan?.payable ?: 0.0, PayMethod.WXPAY)

        launch {
            toast(R.string.request_order)

            try {
                val aliOrder = withContext(Dispatchers.IO) {
                    account.aliPlaceOrder(tier, cycle)
                }

                showProgress(false)

                if (aliOrder == null) {
                    checkOutViewModel.enableInput(true)
                    toast(R.string.order_cannot_be_created)

                    tracker.buyFail(tier)

                    return@launch
                }

                info("Ali order: $aliOrder")

                // Save this subscription data.
                val subs = Subscription(
                        orderId = aliOrder.ftcOrderId,
                        tier = tier,
                        cycle = cycle,
                        cycleCount = plan?.cycleCount ?: 1,
                        extraDays = plan?.extraDays ?: 0,
                        payMethod = PayMethod.ALIPAY,
                        netPrice = aliOrder.netPrice,
                        isUpgrade = plan?.isUpgrade ?: false
                )

                info("Save subscription order: $subs")
                orderManager.save(subs)

                /**
                 * Result is a map:
                 * {resultStatus=6001, result=, memo=操作已经取消。}
                 * {resultStatus=4000, result=, memo=系统繁忙，请稍后再试}
                 * See https://docs.open.alipay.com/204/105301/ in section 同步通知参数说明
                 * NOTE result field is JSON but you cannot use it as JSON.
                 * You could only use it as a string
                 */
                val payResult = withContext(Dispatchers.IO) {
                    PayTask(this@CheckOutActivity).payV2(aliOrder.param, true)
                }

                info("Alipay result: $payResult")

                val resultStatus = payResult["resultStatus"]
                val msg = payResult["memo"] ?: getString(R.string.wxpay_failed)

                if (resultStatus != "9000") {

                    toast(msg)
                    checkOutViewModel.enableInput(true)

                    tracker.buyFail(tier)

                    return@launch
                }

                toast(R.string.wxpay_done)

                confirmAlipay(account, subs)

            } catch (e: ClientError) {
                info(e)
                showProgress(false)
                checkOutViewModel.enableInput(true)

                handleClientError(e)

                tracker.buyFail(tier)

            } catch (e: Exception) {
                info("API error when requesting Ali order: $e")

                showProgress(false)
                checkOutViewModel.enableInput(true)

                handleException(e)

                tracker.buyFail(tier)
            }
        }
    }

    private suspend fun confirmAlipay(account: Account, subs: Subscription) {

        tracker.buySuccess(subs)

        val updatedMembership = subs.confirm(account.membership)

        info("New membership: $updatedMembership")

        sessionManager.updateMembership(updatedMembership)

        toast(R.string.progress_refresh_account)

        val refreshedAccount = withContext(Dispatchers.IO) {
            account.refresh()
        }

        showProgress(false)

        if (refreshedAccount == null) {
            toast(R.string.order_not_found)
            return
        }

        toast(R.string.prompt_updated)

        /**
         * If remote membership is newer than local
         * one, save remote data; otherwise do
         * nothing in case server notification comes
         * late.
         */
        if (refreshedAccount.membership.isNewer(updatedMembership)) {
            sessionManager.saveAccount(refreshedAccount)
        }

        setResult(Activity.RESULT_OK)

        MemberActivity.start(this)

        finish()
    }

    private fun stripePay() {
        toast("Stripe pay is not supported yet.")
    }

    // This might never be used.
    private fun directUpgrade() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)

            return
        }

        val account = sessionManager.loadAccount() ?: return

        showProgress(true)

        launch {
            try {
                val (ok, plan) = withContext(Dispatchers.IO) {
                    account.directUpgrade()
                }

                showProgress(true)

                if (ok) {
                    toast("Upgrade done")
                    return@launch
                }

                checkOutViewModel.enableInput(true)

                if (plan == null) {
                    toast("An error occurred while upgrading. Please try later")
                    return@launch
                }

                if (plan.payable > 0.0) {
                    toast("You current balance is not enough to cover upgrading price.")
                    return@launch
                }

            } catch (e: ClientError) {
                info(e)

                showProgress(false)
                checkOutViewModel.enableInput(true)

                when (e.statusCode) {
                    404 -> toast(R.string.api_member_not_found)
                    else -> handleApiError(e)
                }
            } catch (e: Exception) {
                showProgress(false)
                checkOutViewModel.enableInput(true)

                handleException(e)
            }
        }
    }

    private fun handleClientError(resp: ClientError) {
        when (resp.statusCode) {
            403 -> {
                toast(R.string.renewal_not_allowed)
            }
            else -> {
                handleApiError(resp)
            }
        }
    }

    companion object {
        private const val EXTRA_PLAN_PAYABLE = "extra_plan_payable"

        fun startForResult(activity: Activity?, requestCode: Int, p: PlanPayable) {
            val intent = Intent(activity, CheckOutActivity::class.java).apply {
                putExtra(EXTRA_PLAN_PAYABLE, p)
            }

            activity?.startActivityForResult(intent, requestCode)
        }
    }
}
