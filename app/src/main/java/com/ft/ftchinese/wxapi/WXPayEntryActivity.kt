package com.ft.ftchinese.wxapi

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.*
import com.ft.ftchinese.user.SubscriptionActivity
import com.ft.ftchinese.util.handleException
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.activity_wx_pay_result.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class WXPayEntryActivity: AppCompatActivity(), IWXAPIEventHandler, AnkoLogger {
    private var api: IWXAPI? = null
    private var job: Job? = null
    private var mSession: SessionManager? = null

    private var isInProgress: Boolean = false
        set(value) {
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

    private var isSuccess: Boolean = true
        set(value) {
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

        api = WXAPIFactory.createWXAPI(this, BuildConfig.WECAHT_APP_ID)

        mSession = SessionManager.getInstance(this)

        // Only used to test WXPayEntryActivity's UI.
        // Comment them for production.
//        val isUiTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
//
//        if (isUiTest) {
//            val member = Membership(
//                    tier = Membership.TIER_STANDARD,
//                    billingCycle = Membership.CYCLE_MONTH,
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
                    isInProgress = false
                    heading_tv.text = getString(R.string.wxpay_error)
                }
                // 用户取消
                // 无需处理。发生场景：用户不支付了，点击取消，返回APP。
                -2 -> {
                    isInProgress = false
                    heading_tv.text = getString(R.string.wxpay_cancelled)
                }
            }
        }
    }

    private fun verifyOrder() {
        val subs = Subscription.load(this) ?: return

        val user = mSession?.loadUser() ?: return

        isInProgress = true

        job = GlobalScope.launch {

            try {
                val payResult = user.wxQueryOrder(subs.orderId)

                isInProgress = false

                // confirmedAt is ISO8601 string.
                subs.confirmedAt = payResult.paidAt

                handleQueryResult(payResult, subs)

            } catch (resp: ErrorResponse) {
                isInProgress = false

                handleApiErr(resp)
            } catch (e: Exception) {
                e.printStackTrace()

                isInProgress = false
                handleException(e)
            }
        }
    }

    private fun handleQueryResult(queryResult: WxQueryOrder, subs: Subscription) {

        val currentMember = mSession?.loadUser()?.membership ?: return

        var resultText = ""

        when (queryResult.paymentState) {
            // If payment success, update user sessions's member tier, expire date, billing cycle.
            "SUCCESS" -> {
                isSuccess = true
                resultText = getString(R.string.wxpay_done)


                val member = subs.updateMembership(currentMember)

                info("New membership: $member")

                updateUI(member)
                updateSession(member)

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

        val tierText = when (member.tier) {
            Membership.TIER_STANDARD -> getString(R.string.member_tier_standard)
            Membership.TIER_PREMIUM -> getString(R.string.member_tier_premium)
            else -> ""
        }

        val cycleText = when (member.billingCycle) {
            Membership.CYCLE_YEAR -> getString(R.string.billing_cycle_year)
            Membership.CYCLE_MONTH -> getString(R.string.billing_cycle_month)
            else -> ""
        }

        tier_tv.text = getString(R.string.wxpay_member_tier, tierText, cycleText)

        expire_tv.text = getString(R.string.wxpay_exppire_date, member.expireDate)
    }

    private fun handleApiErr(resp: ErrorResponse) {
        when (resp.statusCode) {
            400 -> {
                toast(R.string.api_bad_request)
            }
            401 -> {
                toast(R.string.api_unauthorized)
            }
            404 -> {
                toast(R.string.order_not_found)
            }
            422 -> {
                toast(resp.message)
            }
            else -> {
                toast(R.string.api_server_error)
            }
        }
    }

    private fun updateSession(membership: Membership) {
        mSession?.updateMembership(membership)
    }

    override fun onReq(req: BaseReq?) {

    }

    fun onClickDone(view: View) {
        // Start MembershipActivity manually here.
        // This is the only way to refresh user data.
        SubscriptionActivity.start(this, true)

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
        SubscriptionActivity.start(this, true)
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