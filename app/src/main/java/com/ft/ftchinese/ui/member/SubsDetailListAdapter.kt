package com.ft.ftchinese.ui.member

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.ui.lists.TwoEqualColViewHolder

class SubsDetailListAdapter : RecyclerView.Adapter<TwoEqualColViewHolder>() {

    private var rows: List<Pair<String, String>> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoEqualColViewHolder {
        return TwoEqualColViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: TwoEqualColViewHolder, position: Int) {
        val pair = rows[position]
        holder.setLeadingText(pair.first)
        holder.setTrailingText(pair.second)
    }

    override fun getItemCount() = rows.size

    fun setData(pairs: List<Pair<String, String>>) {
        this.rows = pairs
        notifyDataSetChanged()
    }
}
