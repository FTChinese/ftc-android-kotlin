package com.ft.ftchinese.ui.member

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMyOrdersBinding
import com.ft.ftchinese.model.fetch.formatISODateTime
import com.ft.ftchinese.model.subscription.Order
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.formatter.formatPrice
import com.ft.ftchinese.ui.formatter.formatTierCycle
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
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
        setSupportActionBar(binding.toolbar.toolbar)

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

        viewModel.ordersResult.observe(this) {
            onOrdersFetch(it)
        }

        if (!isConnected) {

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
            val tierCycle = formatTierCycle(this, it.tier, it.cycle)

            val payMethod = getString(it.payMethod.stringRes)

            val price = formatPrice(this, it.priceParams)

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

