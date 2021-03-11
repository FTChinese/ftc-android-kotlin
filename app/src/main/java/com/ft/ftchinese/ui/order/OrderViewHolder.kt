package com.ft.ftchinese.ui.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

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
            return OrderViewHolder(
                LayoutInflater
                .from(parent.context)
                .inflate(R.layout.card_order, parent, false))
        }
    }
}
