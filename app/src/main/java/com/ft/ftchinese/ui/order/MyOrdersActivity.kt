package com.ft.ftchinese.ui.order

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ActivityMyOrdersBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedAppActivity
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.viewmodel.AccountViewModel
import org.jetbrains.anko.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class MyOrdersActivity : ScopedAppActivity() {

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

        viewModel = ViewModelProvider(this)[AccountViewModel::class.java]

        connectionLiveData.observe(this) {
            viewModel.isNetworkAvailable.value = it
        }
        viewModel.isNetworkAvailable.value = isConnected

        viewModel.progressLiveData.observe(this) {
            binding.inProgress = it
        }

        viewModel.ordersResult.observe(this) { result ->
            when (result) {
                is FetchResult.LocalizedError -> {
                    toast(result.msgId)
                }
                is FetchResult.Error -> {
                    result.exception.message?.let { toast(it) }
                }
                is FetchResult.Success -> {
                    viewAdapter.setData(result.data)
                    viewAdapter.notifyDataSetChanged()
                }
            }
        }

        sessionManager.loadAccount()?.let {
            viewModel.fetchOrders(it)
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

