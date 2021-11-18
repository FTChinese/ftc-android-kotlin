package com.ft.ftchinese.ui.order

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.model.ftcsubs.Order
import com.ft.ftchinese.ui.formatter.FormatHelper
import org.threeten.bp.format.DateTimeFormatter

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
                FormatHelper.formatEdition(it, order.edition),
            )
            holder.amountView.text = it.getString(
                R.string.order_price,
                FormatHelper.formatPrice(it, order.currency, order.amount),
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

