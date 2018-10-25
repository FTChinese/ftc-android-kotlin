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
import android.widget.Button
import android.widget.TextView
import com.ft.ftchinese.R
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

private const val VIEW_TYPE_SEPARATOR = 0x01
private const val VIEW_TYPE_ITEM = 0x02
private const val VIEW_TYPE_TEXT = 0x03

/**
 * Show user's account details
 */
class AccountActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        val user = SessionManager.getInstance(this).loadUser()
        return AccountFragment.newInstance(user)
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

internal class AccountFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var mUser: Account? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null
    private var mAdapter: Adapter? = null

    private var isInProgress: Boolean = false
        set(value) {
            mListener?.onProgress(value)
        }

    /**
     * Update refresh retrieve mUser data from API, save it and update ui.
     */
    override fun onRefresh() {
        toast(R.string.action_updating)

        job = launch(UI) {
            try {
                val user = mUser?.refreshAsync()?.await()
                channel_fragment_swipe.isRefreshing = false

                mUser = user
                updateUI()

                if (user != null) {
                    mListener?.onUserSession(user)
                }

                toast(R.string.success_updated)
            } catch (e: ErrorResponse) {
                when (e.statusCode) {
                    404 -> {
                        toast("用户不存在")
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                channel_fragment_swipe.isRefreshing = false
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            mListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val userData = it.getString(ARG_USER_DATA)
            mUser = try {
                gson.fromJson<Account>(userData, Account::class.java)
            } catch (e: Exception) {
                null
            }
        }
        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView")
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set refresh mListener
        channel_fragment_swipe.setOnRefreshListener(this)

        // Set up recycler view.
        recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)

            // Add divider
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        info("onViewCreated")
    }

    override fun onResume() {
        super.onResume()

        info("onResume")
        updateUI()
    }

    private fun updateUI() {
        info("updateUI")

        val accountItems = AccountItem.create(mUser)

        if (mAdapter == null) {
            mAdapter = Adapter(accountItems)
            recycler_view.adapter = mAdapter
        } else {
            mAdapter?.setItems(accountItems)
            mAdapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        private const val ARG_USER_DATA = "user_data"
        fun newInstance(user: Account?) =
                AccountFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_USER_DATA, gson.toJson(user))
                    }
                }
    }

    inner class TextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageView: TextView? = itemView.findViewById(R.id.message_view)
        val actionButton: Button? = itemView.findViewById(R.id.action_button)
    }

    inner class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView? = itemView.findViewById(R.id.label_text)
        val valueView: TextView? = itemView.findViewById(R.id.value_text)
    }

    inner class SeparatorViewHolder(view: View) : RecyclerView.ViewHolder(view)

    inner class Adapter(private var mItems: List<AccountItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

            return when (viewType) {
                VIEW_TYPE_TEXT -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.list_item_text_button, parent, false)

                    TextViewHolder(view)
                }
                VIEW_TYPE_SEPARATOR -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.list_item_separator, parent, false)
                    SeparatorViewHolder(view)
                }
                else -> {
                    val view = LayoutInflater.from(parent.context)
                            .inflate(R.layout.list_item_table_row, parent, false)
                    ItemViewHolder(view)
                }
            }
        }

        override fun getItemCount(): Int {
            return mItems.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val item = mItems[position]

            when (holder) {
                is TextViewHolder -> {
                    holder.messageView?.text = item.label
                    holder.actionButton?.text = item.value

                    holder.actionButton?.setOnClickListener {
                        toast("Request verification")
                    }
                }
                is ItemViewHolder -> {
                    holder.labelView?.text = item.label
                    holder.valueView?.text = if (item.value.isNullOrBlank()) "未设置" else item.value

                    holder.itemView.setOnClickListener {

                        if (item.id == null) {
                            return@setOnClickListener
                        }

                        AccountUpdateActivity.start(context, item.id)

                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return mItems[position].viewType
        }

        fun setItems(items: List<AccountItem>) {
            mItems = items
        }

        override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
            super.onViewAttachedToWindow(holder)
        }
    }

    private fun resendVerification() {
        val uuid = mUser?.id ?: return

        isInProgress = true

        job = launch(UI) {
            try {
                sendRequest(uuid)

                toast("邮件已发送")
            } catch (e: ErrorResponse) {
                isInProgress = false

                when (e.statusCode) {
                    403 -> {
                        toast("Access Forbidden")
                    }
                    404 -> {
                        toast("用户不存在")
                    }
                    500 -> {
                        toast("服务器出错了")
                    }
                }

            } catch (e: Exception) {
                isInProgress = false

                handleException(e)
            }
        }
    }

    private suspend fun sendRequest(uuid: String) {
        val job = async {
            Fetch()
                    .post(NextApi.REQUEST_VERIFICATION)
                    .noCache()
                    .setUserId(uuid)
                    .end()
        }

        val response = job.await()

        if (response.code() == 204) {
            info("Email sent")
        } else {
            info("Failed to updateAsync email. ${response.code()}: ${response.message()}")
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

        fun create(user: Account?): List<AccountItem> {
            val items = mutableListOf(
                    AccountItem(label = "您的邮箱尚未验证，为保障您的账号安全，请及时验证邮箱。我们已经给您的登录邮箱发送了验证邮件，点击邮件中的链接即可。", value = "重新发送验证邮件", viewType = VIEW_TYPE_TEXT),
                    AccountItem(label = "账号", viewType = VIEW_TYPE_SEPARATOR),
                    AccountItem(label = "邮箱", value = user?.email, viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_EMAIL),
                    AccountItem(label = "用户名", value = user?.userName, viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_USER_NAME),
                    AccountItem(label = "密码", value = "修改密码", viewType = VIEW_TYPE_ITEM, id = AccountItem.ID_PASSWORD),
                    AccountItem(label = "账号绑定", viewType = VIEW_TYPE_SEPARATOR),
                    AccountItem(label = "微信", value = "尚未绑定", viewType = VIEW_TYPE_ITEM)
            )

            if (user?.isVerified == true) {
                items.removeAt(0)
            }
            return items
        }
    }
}
