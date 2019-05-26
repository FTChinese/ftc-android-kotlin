package com.ft.ftchinese.ui.pay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.card_order.view.*

class OrderAdapter(private var orders: List<OrderRow>) :
        RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    inner class ViewHolder (view: View): RecyclerView.ViewHolder(view) {
        val orderIdView: TextView = view.order_id_tv
        val tierCycleView: TextView = view.tier_cycle_tv
        val durationView: TextView = view.duration_tv
        val netPriceView: TextView = view.net_price_tv
        val payMethodView: TextView = view.pay_method_tv
        val createdAtView: TextView = view.created_at_tv
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