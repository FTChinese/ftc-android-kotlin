package com.ft.ftchinese.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.ui.formatter.formatEdition
import com.ft.ftchinese.ui.formatter.getCurrencySymbol
import org.threeten.bp.format.DateTimeFormatter

class OrderViewHolder (view: View): RecyclerView.ViewHolder(view) {
    val orderIdView: TextView = view.findViewById(R.id.order_id_tv)
    val planView: TextView = view.findViewById(R.id.order_subscribed_plan)
    val amountView: TextView = view.findViewById(R.id.tv_order_amount)
    val payMethodView: TextView = view.findViewById(R.id.tv_payment_method)
    val createdAtView: TextView = view.findViewById(R.id.tv_creation_time)
    val periodView: TextView = view.findViewById(R.id.tv_order_period)

    companion object {
        @JvmStatic
        fun create(parent: ViewGroup): OrderViewHolder {
            return OrderViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.card_order, parent, false))
        }
    }
}

class OrderAdapter(private var orders: List<Order>) : RecyclerView.Adapter<OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        return OrderViewHolder.create(parent)
    }

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.itemView.context.let {
            holder.orderIdView.text = it.getString(
                R.string.order_id,
                order.id)
            holder.planView.text = it.getString(
                R.string.order_subscribed_plan,
                formatEdition(it, order.edition),
            )
            holder.amountView.text = it.getString(
                R.string.order_price,
                it.getString(
                    R.string.formatter_price,
                    getCurrencySymbol(order.currency),
                    order.amount
                ),
            )
            holder.payMethodView.text = it.getString(
                R.string.order_pay_method,
                order.payMethod,
            )
            holder.createdAtView.text = it.getString(
                R.string.order_creation_time,
                order.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )
            holder.periodView.text = it.getString(
                R.string.order_period,
                order.startDate,
                order.endDate,
            )
        }

    }

    fun setData(orders: List<Order>) {
        this.orders = orders
    }
}

