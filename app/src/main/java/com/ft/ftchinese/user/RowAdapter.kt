package com.ft.ftchinese.user

import android.graphics.Typeface
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.R

/**
 * A two column table-like recycler view.
 */
class RowAdapter(private var rows: Array<TableRow>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<RowAdapter.ViewHolder>() {

    class ViewHolder (view: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val labelView: TextView = view.findViewById(R.id.label_tv)
        val valueView: TextView = view.findViewById(R.id.value_tv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_table_row, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = rows[position]
        holder.labelView.text = row.header
        holder.valueView.text = row.data
        if (row.color != null) {
            holder.valueView.setTextColor(row.color)
        }

        if (row.isBold) {
            holder.valueView.setTypeface(holder.valueView.typeface, Typeface.BOLD)
        }
    }

    fun refreshData(rows: Array<TableRow>) {
        this.rows = rows
    }
}

data class TableRow(
        val header: String,
        val data: String,
        val isBold: Boolean = false,
        val color: Int? = null
)