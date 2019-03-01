package com.ft.ftchinese.wxapi

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.MySubsActivity
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wechat.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class WXPayEntryActivity: AppCompatActivity(), IWXAPIEventHandler, AnkoLogger {
    private var api: IWXAPI? = null
    private var job: Job? = null
    private var mSession: SessionManager? = null
    private var mOrderManager: OrderManager? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    private fun showProgress(value: Boolean) {
        if (value) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wechat)

        setSupportActionBar(toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)

        mSession = SessionManager.getInstance(this)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        showUI(false)

        // Only used to test WXPayEntryActivity's UI.
        // Comment them for production.
//        val isUiTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
//
//        if (isUiTest) {
//            val member = Membership(
//                    tier = Membership.TIER_STANDARD,
//                    cycle = Membership.CYCLE_MONTH,
//                    expireDate = "2018-12-12"
//            )
//            isInProgress = false
//            isSuccess = true
//            heading_tv.text = getString(R.string.wxpay_done)
//            updateUI(member)
//
//            return
//        } else {
//            isInProgress = false
//            heading_tv.text = getString(R.string.wxpay_cancelled)
//            return
//        }

        api?.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        api?.handleIntent(intent, this)
    }

    // Reference https://pay.weixin.qq.com/wiki/doc/api/app/app.php?chapter=8_5
    override fun onResp(resp: BaseResp?) {
        info("onPayFinish, errCode = ${resp?.errCode}")

        if (resp?.type == ConstantsAPI.COMMAND_PAY_BY_WX) {
            when (resp.errCode) {
                // 成功
                // 展示成功页面
                0 -> {
                    verifyOrder()
                }
                // 错误
                // 可能的原因：签名错误、未注册APPID、项目设置APPID不正确、注册的APPID与设置的不匹配、其他异常等。
                -1 -> {
                    showProgress(false)
                    heading_tv.text = getString(R.string.wxpay_error)
                }
                // 用户取消
                // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                -2 -> {
                    showProgress(false)
                    heading_tv.text = getString(R.string.wxpay_cancelled)
                }
            }
        }
    }

    private fun verifyOrder() {
        val subs = mOrderManager?.load() ?: return

        val user = mSession?.loadAccount() ?: return

        showProgress(true)

        job = GlobalScope.launch(Dispatchers.Main) {

            try {
                val payResult = withContext(Dispatchers.IO) {
                    user.wxQueryOrder(subs.orderId)
                }

                showProgress(false)

                if (payResult == null) {
                    return@launch
                }

                handleQueryResult(payResult, subs)

            } catch (resp: ClientError) {
                showProgress(false)

                when (resp.statusCode) {
                    404 -> {
                        toast(R.string.order_not_found)
                    }
                    else -> {
                        handleApiError(resp)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                showProgress(false)
                handleException(e)
            }
        }
    }

    private fun handleQueryResult(queryResult: WxQueryOrder, subs: Subscription) {

        val currentMember = mSession?.loadAccount()?.membership ?: return

        var resultText = ""

        when (queryResult.paymentState) {
            // If payment success, update user sessions's member tier, expire date, billing cycle.
            "SUCCESS" -> {

                val updatedMember = subs.confirm(currentMember)

                info("New membership: $updatedMember")

                showSuccessUI(getString(R.string.wxpay_done))
                mSession?.updateMembership(updatedMember)


                logPurchaseEvent(subs)

                return
            }
            "REFUND" -> {
                resultText = getString(R.string.wxpay_refund)
            }
            "NOTPAY" -> {
                resultText = getString(R.string.wxpay_not_paid)
            }
            "CLOSED" -> {
                resultText = getString(R.string.wxpay_closed)
            }
            "REVOKED" -> {
                resultText = getString(R.string.wxpay_revoked)
            }
            "USERPAYING" -> {
                resultText = getString(R.string.wxpay_pending)
            }
            "PAYERROR" -> {
                resultText = getString(R.string.wxpay_failed)
            }
        }

        showFailureUI(resultText)
    }

    private fun showUI(show: Boolean) {
        if (show) {
            heading_tv.visibility = View.VISIBLE
            done_button.visibility = View.VISIBLE
        } else {
            heading_tv.visibility = View.GONE
            done_button.visibility = View.GONE
        }
    }


    private fun showSuccessUI(heading: String) {
        showUI(true)
        heading_tv.text = heading
        done_button.setOnClickListener {
            // Start MembershipActivity manually here.
            // This is the only way to refresh user data.
            MySubsActivity.start(this)

            finish()
        }
    }

    private fun showFailureUI(heading: String) {
        showUI(true)

        heading_tv.text = heading

        done_button.setOnClickListener {
            SubscriptionActivity.start(this)
            finish()
        }
    }

    // Log purchase event.
    private fun logPurchaseEvent(subs: Subscription) {

        mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
            putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
            putDouble(FirebaseAnalytics.Param.VALUE, subs.netPrice)
            putString(FirebaseAnalytics.Param.METHOD, subs.payMethod.string())
        })
    }

    override fun onReq(req: BaseReq?) {

    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    // Force back button to startForResult MembershipActivity so that use feels he is returning to previous MembershipActivity while actually the old instance already killed.
    // This hacking is used to refresh user data.
    // On iOS you do not need to handle it since there's no back button.
    override fun onBackPressed() {
        super.onBackPressed()
        SubscriptionActivity.start(this, null)
        finish()
    }

    // For test only.
//    companion object {
//        private const val EXTRA_IS_TEST = "ui_test"
//        fun startForResult(activity: Activity?) {
//            val intent = Intent(activity, WXPayEntryActivity::class.java)
//            intent.putExtra(EXTRA_IS_TEST, false)
//
//            activity?.startActivity(intent)
//        }
//    }
}