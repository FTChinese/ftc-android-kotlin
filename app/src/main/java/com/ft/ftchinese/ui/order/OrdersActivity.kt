package com.ft.ftchinese.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMyOrdersBinding
import com.ft.ftchinese.model.subscription.Order
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
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
                viewAdapter.setData(result.data)
                viewAdapter.notifyDataSetChanged()
            }
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

