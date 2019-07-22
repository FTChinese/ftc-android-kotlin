package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.account_row.view.*
import kotlin.reflect.KClass

data class AccountItem(
    val label: String,
    val value: String,
    val activityClass: KClass<Activity>
)

class AccountRowAdapter(
        private val context: Context,
        private var items: Array<AccountItem>
) : RecyclerView.Adapter<AccountRowAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView = view.tv_start
        val valueView: TextView = view.tv_end
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.account_row, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.labelView.text = item.label
        holder.valueView.text = item.value

        holder.itemView.setOnClickListener {
            context.startActivity(Intent(context, item.activityClass.java))
        }
    }
}
