package com.ft.ftchinese.ui.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.account.CustomerActivity
import com.ft.ftchinese.ui.account.UpdateActivity
import com.ft.ftchinese.ui.account.WxInfoActivity
import com.google.android.material.button.MaterialButton
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast

private const val TYPE_BANNER = 1
private const val TYPE_SETTING = 2

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AccountAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), AnkoLogger {

    private var rows = listOf<AccountRow>()


    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.findViewById(R.id.tv_primary)
        private val secondaryView: TextView = view.findViewById(R.id.tv_secondary)
        private val context = itemView.context
        private var row: AccountRow? = null

        init {
            itemView.setOnClickListener {
                when (row?.id) {
                    AccountRowType.EMAIL,
                    AccountRowType.PASSWORD,
                    AccountRowType.USER_NAME -> UpdateActivity.start(context, row?.id)
                    AccountRowType.STRIPE -> CustomerActivity.start(context)
                    AccountRowType.WECHAT -> WxInfoActivity.start(context)
                    else -> {
                        context.toast("No idea how to handle the row you clicked: ${row?.primary}")
                    }
                }
            }
        }

        fun bind(item: AccountRow) {
            this.row = item
            titleView.text = item.primary
            secondaryView.text = item.secondary
        }
    }

    class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val tvMessage: TextView = view.findViewById(R.id.banner_message)
        private val btnPositive: MaterialButton = view.findViewById(R.id.btn_send_request)
        private var row: AccountRow? = null

        init {
            btnPositive.setOnClickListener {
                // How to do next?
            }
        }

        fun bind(item: AccountRow) {
            this.row = item
            tvMessage.text = item.primary
            btnPositive.text = item.secondary
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        info("view type: $viewType")

        if (viewType == TYPE_BANNER) {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.banner, parent, false)

            return BannerViewHolder(view)
        }

        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.account_list_item, parent, false)


        return ItemViewHolder(view)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = rows[position]

        if (getItemViewType(position) == TYPE_BANNER) {
            if (holder is BannerViewHolder) {
                holder.bind(item)
            }
        } else {
            if (holder is ItemViewHolder) {
                holder.bind(item)
            }
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (rows[position].id == AccountRowType.REQUEST_VERIFICATION) {
            TYPE_BANNER
        } else {
            TYPE_SETTING
        }
    }

    fun setItems(items: List<AccountRow>) {
        this.rows = items
    }
}

data class AccountRow(
        val id: AccountRowType,
        val primary: String,
        val secondary: String
)

enum class AccountRowType {
    REQUEST_VERIFICATION,
    EMAIL,
    USER_NAME,
    PASSWORD,
    STRIPE,
    WECHAT
}
