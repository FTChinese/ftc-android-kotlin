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

    fun setPadding(space: Int) {
        itemView.setPadding(space, space, space, space)
    }

    fun setPadding(leftRight: Int, topBottom: Int) {
        itemView.setPadding(leftRight, topBottom, leftRight, topBottom)
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        itemView.setPadding(left, top, right, bottom)
    }

    companion object {
        fun create(parent: ViewGroup): SingleLineItemViewHolder {
            return SingleLineItemViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_single_line, parent, false))
        }
    }
}
