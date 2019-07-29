package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.LoginMethod
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.ui.login.AccountResult
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.banner.view.*
import kotlinx.android.synthetic.main.fragment_ftc_account_v2.*
import kotlinx.android.synthetic.main.list_item.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.toast

private const val TYPE_BANNER = 1
private const val TYPE_SETTING = 2

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FtcAccountV2Fragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel

    private var viewAdapter: AccountAdapter? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_ftc_account_v2, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = LinearLayoutManager(context)

        viewAdapter = AccountAdapter()

        account_list_rv.apply {
            setHasFixedSize(true)
            layoutManager = layout
            adapter = viewAdapter


        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()

        if (viewAdapter == null) {
            viewAdapter = AccountAdapter()
            account_list_rv.adapter = viewAdapter
        } else {

            updateUI()
        }
    }

    private fun updateUI() {
        viewAdapter?.apply {
            setItems(buildRows())
            notifyDataSetChanged()
        }
    }

    private fun buildRows(): List<AccountRow> {
        val account = sessionManager.loadAccount() ?: return listOf()


        return listOf(
                AccountRow(
                        id = AccountRowType.EMAIL,

                        primary = if (account.isVerified) getString(R.string.label_email) else getString(R.string.email_not_verified),
                        secondary = if (account.email.isNotBlank()) {
                            account.email
                        } else {
                            getString(R.string.prompt_not_set)
                        }
                ),
                AccountRow(
                        id = AccountRowType.USER_NAME,
                        primary = getString(R.string.label_user_name),
                        secondary = if (account.userName.isNullOrBlank()) {
                            getString(R.string.prompt_not_set)
                        } else {
                            account.userName
                        }
                ),
                AccountRow(
                        id = AccountRowType.PASSWORD,
                        primary = getString(R.string.label_password),
                        secondary = "********"
                ),
                AccountRow(
                        id = AccountRowType.STRIPE,
                        primary = "Stripe钱包",
                        secondary = "添加银行卡或设置默认支付方式"
                ),
                AccountRow(
                        id = AccountRowType.WECHAT,
                        primary = getString(R.string.label_wechat),
                        secondary = if (account.isLinked) {
                            getString(R.string.action_bound_account)
                        } else {
                            getString(R.string.action_bind_account)
                        }
                )
        )


    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        accountViewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        swipe_refresh.setOnRefreshListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                stopRefreshing()
                return@setOnRefreshListener
            }

            val acnt = sessionManager.loadAccount()

            if (acnt == null) {
                toast("Account not found")
                stopRefreshing()
                return@setOnRefreshListener
            }

            toast(R.string.refreshing_account)

            accountViewModel.refresh(acnt)
        }
    }

    private fun onAccountRefreshed(accountResult: AccountResult?) {
        stopRefreshing()

        if (accountResult == null) {
            return
        }

        if (accountResult.error != null) {
            toast(accountResult.error)
            return
        }

        if (accountResult.exception != null) {
            activity?.handleException(accountResult.exception)
            return
        }

        if (accountResult.success == null) {
            toast("Unknown error")
            return
        }

        toast(R.string.prompt_updated)

        sessionManager.saveAccount(accountResult.success)

        if (accountResult.success.isWxOnly) {
            info("A wechat only account. Switch UI.")
            accountViewModel.switchUI(LoginMethod.WECHAT)
            return
        }

        updateUI()
    }

    private fun stopRefreshing() {
        swipe_refresh.isRefreshing = false
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                FtcAccountV2Fragment()
    }
}

@kotlinx.coroutines.ExperimentalCoroutinesApi
class AccountAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(), AnkoLogger {

    private var rows = listOf<AccountRow>()


    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleView: TextView = view.tv_primary
        private val secondaryView: TextView = view.tv_secondary
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

        private val tvMessage: TextView = view.banner_message
        private val btnPositive: MaterialButton = view.banner_positive_btn
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
                .inflate(R.layout.list_item, parent, false)


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
