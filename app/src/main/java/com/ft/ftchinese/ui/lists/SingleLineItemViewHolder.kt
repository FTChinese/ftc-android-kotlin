package com.ft.ftchinese.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

class SingleLineItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val icon: ImageView = view.findViewById(R.id.list_item_icon)
    val text: TextView = view.findViewById(R.id.list_item_text)
    val disclosure: ImageView = view.findViewById(R.id.list_item_disclosure)

    companion object {
        fun create(parent: ViewGroup): SingleLineItemViewHolder {
            return SingleLineItemViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_single_line, parent, false))
        }
    }
}
