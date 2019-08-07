package com.ft.ftchinese.ui.pay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.getTierCycleText
import com.ft.ftchinese.model.order.OrderManager
import com.ft.ftchinese.model.order.PayMethod
import com.ft.ftchinese.model.order.Subscription
import kotlinx.android.synthetic.main.activity_latest_order.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.threeten.bp.format.DateTimeFormatter

/**
 * [LatestOrderActivity] shows the payment result of alipay of wxpay.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LatestOrderActivity : ScopedAppActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_order)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val orderManager = OrderManager.getInstance(this)

        btn_subscription_done.setOnClickListener {
            MemberActivity.start(this)
            finish()
        }

        val order = orderManager.load() ?: return

        rv_last_order.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LatestOrderActivity)
            adapter = OrderAdapter(buildRows(order))
        }
    }

    private fun buildRows(order: Subscription): List<OrderRow> {
        return listOf(order).map {
            val tierCycle = getTierCycleText(it.tier, it.cycle)

            val payMethod = when (it.payMethod) {
                PayMethod.ALIPAY -> getString(R.string.pay_method_ali)
                PayMethod.WXPAY -> getString(R.string.pay_method_wechat)
                PayMethod.STRIPE -> getString(R.string.pay_method_stripe)
            }

            val price = getString(R.string.formatter_price, "￥", it.amount)

            OrderRow(
                    orderId = getString(R.string.order_id, it.id),
                    plan = getString(R.string.order_subscribed_plan, tierCycle),
                    price = getString(R.string.order_price, price),
                    payMethod = getString(R.string.order_pay_method, payMethod),
                    creationTime = getString(R.string.order_creation_time, it.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                    period = getString(R.string.order_period, it.startDate, it.endDate)
            )
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, LatestOrderActivity::class.java))
        }
    }
}
