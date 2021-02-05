package com.ft.ftchinese.ui.share

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

class SocialShareViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val icon: ImageView = view.findViewById(R.id.share_icon_view)
    val text: TextView = view.findViewById(R.id.share_text_view)

    companion object {
        fun create(parent: ViewGroup): SocialShareViewHolder {
            return SocialShareViewHolder(LayoutInflater
                .from(parent.context)
                .inflate(R.layout.list_item_figure, parent, false))
        }
    }
}
