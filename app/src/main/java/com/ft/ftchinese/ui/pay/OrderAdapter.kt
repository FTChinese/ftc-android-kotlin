package com.ft.ftchinese.ui.pay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

class OrderAdapter(private var orders: List<OrderRow>) :
        RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    class ViewHolder (view: View): RecyclerView.ViewHolder(view) {
        private val orderIdView: TextView = view.findViewById(R.id.order_id_tv)
        private val planView: TextView = view.findViewById(R.id.order_subscribed_plan)
        private val amountView: TextView = view.findViewById(R.id.tv_order_amount)
        private val payMethodView: TextView = view.findViewById(R.id.tv_payment_method)
        private val createdAtView: TextView = view.findViewById(R.id.tv_creation_time)
        private val periodView: TextView = view.findViewById(R.id.tv_order_period)

        fun bind(row: OrderRow) {
            orderIdView.text = row.orderId
            planView.text = row.plan
            amountView.text = row.price
            payMethodView.text = row.payMethod
            createdAtView.text = row.creationTime
            periodView.text = row.period
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.card_order, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = orders[position]

        holder.bind(row)
    }

    fun setData(orders: List<OrderRow>) {
        this.orders = orders
    }
}

data class OrderRow(
        val orderId: String,
        val plan: String,
        val price: String,
        val payMethod: String,
        val creationTime: String,
        val period: String
)
