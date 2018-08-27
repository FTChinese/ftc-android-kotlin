package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.R
import com.ft.ftchinese.util.ApiEndpoint
import kotlinx.android.synthetic.main.activity_profile.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

private const val VIEW_TYPE_TITLE = 0x01
private const val VIEW_TYPE_ITEM = 0x02

class AccountActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return AccountFragment.newInstance()
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

internal class AccountFragment : Fragment(), AnkoLogger {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(accountItems)
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    companion object {
        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView? = itemView.findViewById(R.id.label_view)
        val valueView: TextView? = itemView.findViewById(R.id.value_view)
    }

    inner class TitleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleView: TextView? = view as TextView
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

                holder.itemView.setOnClickListener {
                    if (item.apiUrl == null) {
                        return@setOnClickListener
                    }
                    when (item.id) {
                        AccountItem.ID_EMAIL -> {

                        }
                        AccountItem.ID_USER_NAME -> {

                        }
                        AccountItem.ID_PASSWORD -> {

                        }
                    }
                }
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

internal data class AccountItem(
        val label: String,
        var value: String? = null,
        val viewType: Int,
        val id: Int? = null,
        val apiUrl: String? = null
) {
    companion object {
        const val ID_EMAIL = 1
        const val ID_USER_NAME = 2
        const val ID_PASSWORD = 3
    }
}

internal val accountItems = arrayOf(
        AccountItem(label = "账号", viewType = VIEW_TYPE_TITLE),
        AccountItem(label = "邮箱", value = "未设置", viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_EMAIL, apiUrl = ApiEndpoint.UPDATE_EMAIL),
        AccountItem(label = "用户名", value = "未设置", viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_USER_NAME, apiUrl = ApiEndpoint.UPDATE_USER_NAME),
        AccountItem(label = "密码", value = "修改密码", viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_PASSWORD, apiUrl = ApiEndpoint.UPDATE_PASSWORD),
        AccountItem(label = "账号绑定", viewType = VIEW_TYPE_TITLE),
        AccountItem(label = "微信", value = "尚未绑定", viewType = VIEW_TYPE_ITEM)
)
