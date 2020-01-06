package com.ft.ftchinese.ui.pay

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMyOrdersBinding
import com.ft.ftchinese.model.subscription.Order
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.subscription.PayMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.util.*
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class MyOrdersActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var viewAdapter: OrderAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityMyOrdersBinding
    private lateinit var viewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_orders)
        setSupportActionBar(toolbar)

        sessionManager = SessionManager.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val viewManager = LinearLayoutManager(this)
        viewAdapter = OrderAdapter(listOf())

        binding.ordersRv.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        viewModel = ViewModelProvider(this)
                .get(AccountViewModel::class.java)

        viewModel.ordersResult.observe(this, Observer {
            onOrdersFetch(it)
        })

        if (!isNetworkConnected()) {

            toast(R.string.prompt_no_network)
            return
        }

        val acnt = sessionManager.loadAccount() ?: return
        binding.inProgress = true

        viewModel.fetchOrders(acnt)
    }

    private fun onOrdersFetch(result: Result<List<Order>>) {
        binding.inProgress = false

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                viewAdapter.setData(buildRows(result.data))
                viewAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun buildRows(orders: List<Order>): List<OrderRow> {

        return orders.map {
            val tierCycle = getTierCycleText(it.tier, it.cycle)

            val payMethod = when (it.payMethod) {
                PayMethod.ALIPAY -> getString(R.string.pay_method_ali)
                PayMethod.WXPAY -> getString(R.string.pay_method_wechat)
                PayMethod.STRIPE -> getString(R.string.pay_method_stripe)
            }

            val price = formatPrice(it.currency, it.amount)

            OrderRow(
                    orderId = getString(R.string.order_id, it.id),
                    plan = getString(R.string.order_subscribed_plan, tierCycle),
                    period = getString(R.string.order_period, it.startDate, it.endDate),
                    price = getString(R.string.order_price, price),
                    payMethod = getString(R.string.order_pay_method, payMethod),
                    creationTime = getString(R.string.order_creation_time, formatISODateTime(it.createdAt))
            )
        }
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(
                    Intent(context, MyOrdersActivity::class.java)
            )
        }
    }
}

