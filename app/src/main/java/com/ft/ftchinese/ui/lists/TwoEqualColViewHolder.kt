package com.ft.ftchinese.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

class TwoEqualColViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val leadingText: TextView = view.findViewById(R.id.equal_col_leading)
    private val trailingText: TextView = view.findViewById(R.id.equal_col_trailing)

    fun setLeadingText(text: CharSequence) {
        leadingText.text = text
    }

    fun setTrailingText(text: CharSequence) {
        trailingText.text = text
    }

    companion object {
        fun create(parent: ViewGroup): TwoEqualColViewHolder {
            return TwoEqualColViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.list_item_two_equal_col, parent, false)
            )
        }
    }
}
