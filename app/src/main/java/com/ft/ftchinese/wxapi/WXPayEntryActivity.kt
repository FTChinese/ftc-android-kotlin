package com.ft.ftchinese.wxapi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wx_pay_result.*
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
            done_button.visibility = View.GONE
            heading_tv.visibility = View.GONE
        } else {
            progress_bar.visibility = View.GONE
            done_button.visibility = View.VISIBLE
            heading_tv.visibility = View.VISIBLE
        }
    }

    private fun showSuccesss(value: Boolean) {
        if (value) {
            member_container.visibility = View.VISIBLE
        } else {
            member_container.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_wx_pay_result)

        setSupportActionBar(toolbar)

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WX_SUBS_APPID)

        mSession = SessionManager.getInstance(this)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

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

                handleClientError(resp)
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
                showSuccesss(true)
                resultText = getString(R.string.wxpay_done)


                val updatedMember = subs.confirm(currentMember)

                info("New membership: $updatedMember")

                updateUI(updatedMember)
                mSession?.updateMembership(updatedMember)

                // Log purchase event.
                mFirebaseAnalytics?.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Bundle().apply {
                    putString(FirebaseAnalytics.Param.CURRENCY, "CNY")
                    putDouble(FirebaseAnalytics.Param.VALUE, subs.netPrice)
                    putString(FirebaseAnalytics.Param.METHOD, subs.payMethod.string())
                })

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

        heading_tv.text = resultText
    }

    private fun updateUI(member: Membership) {

        tier_cycle_tv.text = getTierCycleText(member.key)

        expire_tv.text = getString(
                R.string.wxpay_expire_date,
                formatLocalDate(member.expireDate)
        )
    }

    private fun handleClientError(resp: ClientError) {
        when (resp.statusCode) {
            404 -> {
                toast(R.string.order_not_found)
            }
            else -> {
                handleApiError(resp)
            }
        }
    }

    override fun onReq(req: BaseReq?) {

    }

    fun onClickDone(view: View) {
        // Start MembershipActivity manually here.
        // This is the only way to refresh user data.
        SubscriptionActivity.start(this, true, null)

        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    // Force back button to start MembershipActivity so that use feels he is returning to previous MembershipActivity while actually the old instance already killed.
    // This hacking is used to refresh user data.
    // On iOS you do not need to handle it since there's no back button.
    override fun onBackPressed() {
        super.onBackPressed()
        SubscriptionActivity.start(this, true, null)
        finish()
    }

    // For test only.
//    companion object {
//        private const val EXTRA_IS_TEST = "ui_test"
//        fun start(activity: Activity?) {
//            val intent = Intent(activity, WXPayEntryActivity::class.java)
//            intent.putExtra(EXTRA_IS_TEST, false)
//
//            activity?.startActivity(intent)
//        }
//    }
}