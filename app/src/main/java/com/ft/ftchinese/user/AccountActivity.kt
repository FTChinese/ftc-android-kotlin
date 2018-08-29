package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.ft.ftchinese.R
import com.ft.ftchinese.models.User
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
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

internal class AccountFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var user: User? = null
    private var job: Job? = null
    /**
     * Update refresh retrieve user data from API, save it and update ui.
     */
    override fun onRefresh() {
        job = launch(UI) {
            val newUser = user?.refresh()
            val accountItems = AccountItem.create(newUser)
            recycler_view.adapter = Adapter(accountItems)

            newUser?.save(context)

            user = newUser

            swipe_refresh_layout.isRefreshing = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set refresh listener
        swipe_refresh_layout.setOnRefreshListener(this)

        // Load user data from share preferences
        user = User.loadFromPref(context)

        val accountItems = AccountItem.create(user)

        // Set up recycler view.
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = Adapter(accountItems)
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun newInstance(): AccountFragment {
            return AccountFragment()
        }
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView? = itemView.findViewById(R.id.primary_text_view)
        val valueView: TextView? = itemView.findViewById(R.id.secondary_text_view)
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
                            .inflate(R.layout.card_primary_secondary, parent, false)
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

                info(item)

                holder.labelView?.text = item.label
                holder.valueView?.text = if (item.value.isNullOrBlank()) "未设置" else item.value

                holder.itemView.setOnClickListener {

                    if (item.id == null) {
                        return@setOnClickListener
                    }

                    AccountUpdateActivity.start(context, item.id)

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
        }
    }
}

internal data class AccountItem(
        val label: String,
        var value: String? = null,
        val viewType: Int,
        val id: Int? = null
) {
    companion object {
        const val ID_EMAIL = 1
        const val ID_USER_NAME = 2
        const val ID_PASSWORD = 3

        fun create(user: User?): Array<AccountItem> {
            return arrayOf(
                    AccountItem(label = "账号", viewType = VIEW_TYPE_TITLE),
                    AccountItem(label = "邮箱", value = user?.email, viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_EMAIL),
                    AccountItem(label = "用户名", value = user?.name, viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_USER_NAME),
                    AccountItem(label = "密码", value = "修改密码", viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_PASSWORD),
                    AccountItem(label = "账号绑定", viewType = VIEW_TYPE_TITLE),
                    AccountItem(label = "微信", value = "尚未绑定", viewType = VIEW_TYPE_ITEM)
            )
        }
    }
}
