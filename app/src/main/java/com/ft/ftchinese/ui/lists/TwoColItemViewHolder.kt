package com.ft.ftchinese.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import org.jetbrains.anko.find

class TwoColItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val leadingText: TextView = view.findViewById(R.id.two_col_leading)
    private val trailingText: TextView = view.find(R.id.two_col_trailing)

    fun setLeadingText(text: CharSequence) {
        leadingText.text = text
    }

    fun setTrailingText(text: CharSequence) {
        trailingText.text = text
    }

    companion object {
        fun create(parent: ViewGroup): TwoColItemViewHolder {
            return TwoColItemViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.list_item_two_col, parent, false)
            )
        }
    }
}
