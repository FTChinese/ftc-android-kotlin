package com.ft.ftchinese.ui.lists

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

@Deprecated("Use compose ui")
class CardItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val primaryText: TextView = view.findViewById(R.id.card_primary_tv)
    val secondaryText: TextView = view.findViewById(R.id.card_secondary_tv)

    fun setPrimaryText(char: CharSequence) {
        primaryText.text = char
    }

    fun setSecondaryText(char: CharSequence?) {
        if (char != null) {
            secondaryText.text = char
        } else {
            secondaryText.visibility = View.GONE
        }
    }

    companion object {
        fun create(parent: ViewGroup): CardItemViewHolder {
            return CardItemViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(
                        R.layout.list_item_card,
                        parent,
                        false
                    )
            )
        }
    }
}
