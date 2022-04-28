package com.ft.ftchinese.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

@Deprecated("Use compose ui")
class TwoLineItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val leadingIcon: ImageView = view.findViewById(R.id.list_item_leading_icon)
    private val primaryText: TextView = view.findViewById(R.id.list_item_primary_text)
    private val secondaryText: TextView = view.findViewById(R.id.list_item_secondary_text)

    fun setLeadingIcon(resId: Int?) {
        if (resId != null) {
            leadingIcon.setImageResource(resId)
        } else {
            leadingIcon.visibility = View.GONE
        }
    }

    fun setPrimaryText(text: CharSequence) {
        primaryText.text = text
    }

    fun setSecondaryText(text: CharSequence?) {
        if (text != null) {
            secondaryText.text = text
        } else {
            secondaryText.visibility = View.GONE
        }
    }

    companion object {
        fun create(parent: ViewGroup): TwoLineItemViewHolder {
            return TwoLineItemViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.list_item_two_line, parent, false)
            )
        }
    }
}
