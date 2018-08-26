package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.R
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val VIEW_TYPE_TITLE = 0x01
private const val VIEW_TYPE_ITEM = 0x02

class ProfileActivity : AppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = Adapter(accountItems)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }


    companion object {

        fun start(context: Context) {
            val intent = Intent(context, ProfileActivity::class.java)
            context.startActivity(intent)
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView? = itemView.findViewById(R.id.label_view)
        val valueView: TextView? = itemView.findViewById(R.id.value_view)
    }

    inner class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView? = itemView.findViewById(R.id.title_view)
    }

    inner class Adapter(val items: Array<AccountItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            return when (viewType) {
                VIEW_TYPE_TITLE -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.account_title, parent, false)
                    TitleViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.account_item, parent, false)
                    ItemViewHolder(view)
                }
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = items[position]

            if (holder is ItemViewHolder) {
                holder.labelView?.text = item.label
                holder.valueView?.text = item.value
            } else if (holder is TitleViewHolder) {
                holder.titleView?.text = item.label
            }
        }

        override fun getItemViewType(position: Int): Int {
            return items[position].viewType
        }

        override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
            super.onViewAttachedToWindow(holder)

            info("View attached to window: $holder")
        }
    }
}

data class AccountItem(
        val label: String,
        var value: String,
        val viewType: Int
)

val accountItems = arrayOf(
        AccountItem(label = "账号", value = "", viewType = VIEW_TYPE_TITLE),
        AccountItem(label = "邮箱", value = "未设置", viewType = VIEW_TYPE_ITEM),
        AccountItem(label = "用户名", value = "未设置", viewType = VIEW_TYPE_ITEM),
        AccountItem(label = "密码", value = "修改密码", viewType = VIEW_TYPE_ITEM),
        AccountItem(label = "账号绑定", value = "", viewType = VIEW_TYPE_TITLE),
        AccountItem(label = "微信", value = "尚未绑定", viewType = VIEW_TYPE_ITEM)
)
