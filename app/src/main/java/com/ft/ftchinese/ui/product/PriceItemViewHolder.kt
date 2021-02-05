package com.ft.ftchinese.ui.product

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

class PriceItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val text: TextView = view.findViewById(R.id.tv_original_price)
    val primaryButton: Button = view.findViewById(R.id.btn_price_primary)
    val outlineButton: Button = view.findViewById(R.id.btn_price_outline)

    companion object {
        fun create(parent: ViewGroup): PriceItemViewHolder {
            return PriceItemViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_price_button, parent, false))
        }
    }
}
