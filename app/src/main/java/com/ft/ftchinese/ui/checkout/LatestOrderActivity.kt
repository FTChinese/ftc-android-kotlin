package com.ft.ftchinese.ui.checkout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityLatestOrderBinding
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.store.OrderManager
import com.ft.ftchinese.model.subscription.Order
import com.ft.ftchinese.ui.base.formatTierCycle
import com.ft.ftchinese.ui.member.MemberActivity
import com.ft.ftchinese.ui.member.OrderAdapter
import com.ft.ftchinese.ui.member.OrderRow
import org.threeten.bp.format.DateTimeFormatter

/**
 * [LatestOrderActivity] shows the payment result of alipay of wxpay.
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class LatestOrderActivity : ScopedAppActivity() {

    lateinit var binding: ActivityLatestOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_latest_order)

        setSupportActionBar(binding.toolbar.toolbar)
        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val orderManager = OrderManager.getInstance(this)

        binding.btnSubscriptionDone.setOnClickListener {
            MemberActivity.start(this)
            finish()
        }

        val order = orderManager.load() ?: return

        binding.rvLastOrder.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LatestOrderActivity)
            adapter = OrderAdapter(buildRows(order))
        }
    }

    private fun buildRows(order: Order): List<OrderRow> {
        return listOf(order).map {
            val tierCycle = formatTierCycle(this, it.tier, it.cycle)

            val payMethod = getString(it.payMethod.stringRes)

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
