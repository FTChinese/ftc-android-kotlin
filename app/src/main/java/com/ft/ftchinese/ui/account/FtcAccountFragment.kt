package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentFtcAccountBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FtcAccountFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: FragmentFtcAccountBinding

    private var viewAdapter: AccountAdapter? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_ftc_account, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layout = LinearLayoutManager(context)

        viewAdapter = AccountAdapter()

        binding.accountListRv.apply {
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
            binding.accountListRv.adapter = viewAdapter
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
                            getString(R.string.default_not_set)
                        }
                ),
                AccountRow(
                        id = AccountRowType.USER_NAME,
                        primary = getString(R.string.label_user_name),
                        secondary = if (account.userName.isNullOrBlank()) {
                            getString(R.string.default_not_set)
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

        accountViewModel.accountRefreshed.observe(viewLifecycleOwner, Observer {
            onAccountRefreshed(it)
        })

        binding.swipeRefresh.setOnRefreshListener {
            if (context?.isConnected != true) {
                toast(R.string.prompt_no_network)
                binding.swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            val acnt = sessionManager.loadAccount()

            if (acnt == null) {
                toast("Account not found")
                binding.swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            toast(R.string.refreshing_account)

            accountViewModel.refresh(acnt)
        }
    }

    private fun onAccountRefreshed(accountResult: Result<Account>) {
        binding.swipeRefresh.isRefreshing = false

        when (accountResult) {
            is Result.LocalizedError -> {
                toast(accountResult.msgId)
            }
            is Result.Error -> {
                accountResult.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_updated)

                sessionManager.saveAccount(accountResult.data)

                if (accountResult.data.isWxOnly) {
                    info("A wechat only account. Switch UI.")
                    accountViewModel.switchUI(LoginMethod.WECHAT)
                    return
                }
                updateUI()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                FtcAccountFragment()
    }
}


