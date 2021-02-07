package com.ft.ftchinese.ui.lists

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R

class SingleLineAdapter(private val rows: Array<String>) :
        RecyclerView.Adapter<SingleLineAdapter.ViewHolder>() {

    class ViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val textView = LayoutInflater.from(parent.context)
                .inflate(R.layout.single_line_text, parent, false) as TextView

        return ViewHolder(textView)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = rows[position]
    }
}

