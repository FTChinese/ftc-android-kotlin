package com.ft.ftchinese.ui.product

import android.text.Spannable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

@Deprecated("")
class PriceItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val text: TextView = view.findViewById(R.id.tv_offer_desc)
    val primaryButton: Button = view.findViewById(R.id.btn_price_primary)
    val outlineButton: Button = view.findViewById(R.id.btn_price_outline)

    fun setOfferDesc(desc: String?) {
        if (desc.isNullOrBlank()) {
            text.visibility = View.GONE
        } else {
            text.visibility = View.VISIBLE
            text.text = desc
        }
    }

    fun setPrimaryButton(s: Spannable, enabled: Boolean) {
        outlineButton.visibility = View.GONE
        primaryButton.text = s
        primaryButton.isEnabled = enabled
    }

    fun setSecondaryButton(s: Spannable, enabled: Boolean) {
        primaryButton.visibility = View.GONE
        outlineButton.text = s
        outlineButton.isEnabled = enabled
    }

    companion object {
        fun create(parent: ViewGroup): PriceItemViewHolder {
            return PriceItemViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_price_button, parent, false))
        }
    }
}
