package com.ft.ftchinese.ui.base

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.ListItemBinding


/**
 * Two line list.
 */
class ListAdapter(private var rows: List<ListItem>) :
        RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item,
                        parent,
                        false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.bind(rows[position])
    }


    override fun getItemCount() = rows.size

    fun setData(rows: List<ListItem>) {
        this.rows = rows
        notifyDataSetChanged()
    }

    class ViewHolder(
           private val binding: ListItemBinding
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListItem) {
            binding.item = item

            if (item.startIconRes != null) {
                binding.ivIcon.setImageResource(item.startIconRes)
                binding.ivIcon.visibility = View.VISIBLE
            } else {
                binding.ivIcon.visibility = item.iconVisibility
            }

            if (item.alignCenter) {
                binding.tvPrimary.textAlignment = View.TEXT_ALIGNMENT_CENTER
            }

            binding.executePendingBindings()
        }
    }
}

data class ListItem(
        val primaryText: String? = null,
        val secondaryText: String? = null,
        // Customize icon.
        // If not null, iconVisibility is ignored.
        val startIconRes: Int? = null,
        // Default do not show icons
        val iconVisibility: Int = View.GONE,
        // Only for primary text
        val alignCenter: Boolean = false
)
