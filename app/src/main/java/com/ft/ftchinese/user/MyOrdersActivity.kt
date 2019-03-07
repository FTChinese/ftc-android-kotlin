package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.models.PayMethod
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Subscription
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.activity_my_orders.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

class MyOrdersActivity : AppCompatActivity(), AnkoLogger {

    private lateinit var viewAdapter: OrderAdapter
    private lateinit var sessionManager: SessionManager

    private var job: Job? = null

    private fun showProgress(show: Boolean) {
        if (show) {
            progress_bar.visibility = View.VISIBLE
        } else {
            progress_bar.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)
        setSupportActionBar(toolbar)

        sessionManager = SessionManager.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        val viewManager = LinearLayoutManager(this)
        viewAdapter = OrderAdapter(listOf())

        orders_rv.apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }

        fetchData()
    }

    private fun fetchData() {
        if (!isNetworkConnected()) {

            toast(R.string.prompt_no_network)
            return
        }

        showProgress(true)

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val orders = withContext(Dispatchers.IO) {
                    sessionManager.loadAccount()?.getOrders()
                } ?: return@launch

                showProgress(false)

                info("Orders: $orders")

                viewAdapter.setData(buildRows(orders))
                viewAdapter.notifyDataSetChanged()

            } catch (e: ClientError) {
                showProgress(false)
                handleApiError(e)
            } catch (e: Exception) {
                showProgress(false)
                handleException(e)
            }
        }
    }

    private fun buildRows(orders: List<Subscription>): List<OrderRow> {

        return orders.map {
            val tierCycle = getTierCycleText(it.tier, it.cycle)

            val payMethod = when (it.payMethod) {
                PayMethod.ALIPAY -> getString(R.string.pay_method_ali)
                PayMethod.WXPAY -> getString(R.string.pay_method_wechat)
                PayMethod.STRIPE -> getString(R.string.pay_method_stripe)
            }

            val price = getString(R.string.formatter_price, it.netPrice)

            OrderRow(
                    orderId = getString(R.string.order_id, it.orderId),
                    memberType = getString(R.string.order_member_type, tierCycle),
                    duration = getString(R.string.order_duration, it.startDate, it.endDate),
                    price = getString(R.string.order_price, price),
                    payMethod = getString(R.string.order_pay_method, payMethod),
                    creationTime = getString(R.string.order_creation_time, formatISODateTime(it.createdAt))
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(
                    Intent(context, MyOrdersActivity::class.java)
            )
        }
    }
}

class OrderAdapter(private var orders: List<OrderRow>) :
        RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    class ViewHolder (view: View): RecyclerView.ViewHolder(view) {
        val orderIdView: TextView = view.findViewById(R.id.order_id_tv)
        val tierCycleView: TextView = view.findViewById(R.id.tier_cycle_tv)
        val durationView: TextView = view.findViewById(R.id.duration_tv)
        val netPriceView: TextView = view.findViewById(R.id.net_price_tv)
        val payMethodView: TextView = view.findViewById(R.id.pay_method_tv)
        val createdAtView: TextView = view.findViewById(R.id.created_at_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_order, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = orders[position]

        holder.orderIdView.text = row.orderId
        holder.tierCycleView.text = row.memberType
        holder.durationView.text = row.duration
        holder.netPriceView.text = row.price
        holder.payMethodView.text = row.payMethod
        holder.createdAtView.text = row.creationTime
    }

    fun setData(orders: List<OrderRow>) {
        this.orders = orders
    }
}

data class OrderRow(
        val orderId: String,
        val memberType: String,
        val duration: String,
        val price: String,
        val payMethod: String,
        val creationTime: String
)