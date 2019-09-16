package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.ui.login.AccountResult
import kotlinx.android.synthetic.main.fragment_ftc_account_v2.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast


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
            ViewModelProvider(this)
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
            activity?.showException(accountResult.exception)
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


