package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentFtcAccountBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.store.AccountCache
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.ui.customer.CustomerActivity
import com.ft.ftchinese.ui.customer.CustomerViewModel
import com.ft.ftchinese.ui.customer.CustomerViewModelFactory
import com.ft.ftchinese.ui.dialog.AlertDialogFragment
import com.ft.ftchinese.ui.lists.TwoLineItemViewHolder
import com.ft.ftchinese.ui.wxlink.LinkWxDialogFragment
import com.ft.ftchinese.viewmodel.AccountViewModel
import org.jetbrains.anko.support.v4.toast

/**
 * Show a user's account details in a recycler list for email/mobile login.
 * When email is derived from mobile phone, e.g., ending wht @ftchinese.user,
 * the UI should take it as if the email does not exist. It should not show
 * 'not verified' message, nor should it alert user to verify the email.
 * In such case the mobile row should not allow user to open the UpdateActivity to change phone.
 */
class FtcAccountFragment : ScopedFragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: FragmentFtcAccountBinding

    private lateinit var customerViewModel: CustomerViewModel

    private var listAdapter: ListAdapter = ListAdapter()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_ftc_account,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        accountViewModel = activity?.run {
            ViewModelProvider(this)
                .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        customerViewModel = activity?.run {
            ViewModelProvider(
                this,
                CustomerViewModelFactory(
                    FileCache(requireContext()),
                ),
            ).get(CustomerViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        val layout = LinearLayoutManager(context)

        binding.accountListRv.apply {
            setHasFixedSize(true)
            layoutManager = layout
            adapter = listAdapter
        }

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        accountViewModel.accountRefreshed.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = false

            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.TextError -> toast(it.text)
                is FetchResult.Success -> onAccountRefreshed(it.data)
            }
        }

        customerViewModel.customerCreated.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.TextError -> toast(it.text)
                is FetchResult.Success -> {
                    sessionManager.saveStripeId(it.data.id)
                    CustomerActivity.start(context)
                }
            }
        }
    }

    // After account refreshed, email-wechat linking status might be
    // changed (possibly on other platforms).
    // 1. User logged in with email:
    // If previsouly it is email-only, and after refreshing, it is still email-only, only update ui data;
    // If it changed from email-only to linked, only update the ui data.
    // If previsouly unlinked, and after refreshing it is unlinked,
    // this is still an email account, only update the ui data.
    //
    // 2. User logged in with wechat, then this must be a linked account since when using this fragment.
    // If still linked after refreshing, only update the ui data;
    // If unlinked after refreshing, we need to switch to wechat UI.
    private fun onAccountRefreshed(account: Account) {
        toast(R.string.refresh_success)
        sessionManager.saveAccount(account)

        if (account.isWxOnly) {
            accountViewModel.uiSwitched.value = true
            return
        }
        updateUI()
    }

    private fun initUI() {
        updateUI()

        binding.swipeRefresh.setOnRefreshListener {

            val acnt = sessionManager.loadAccount()
            if (acnt == null) {
                binding.swipeRefresh.isRefreshing = false
                return@setOnRefreshListener
            }

            toast(R.string.refreshing_account)
            accountViewModel.refresh(
                account = acnt,
                manual = true,
            )
        }
    }

    // Turns account data to a list of row.
    private fun updateUI() {
        listAdapter.setData(buildAccountRows(requireContext()))
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    inner class ListAdapter : RecyclerView.Adapter<TwoLineItemViewHolder>() {
        private var rows = listOf<AccountRow>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TwoLineItemViewHolder {
            return TwoLineItemViewHolder.create(parent)
        }

        override fun onBindViewHolder(holder: TwoLineItemViewHolder, position: Int) {
            val item = rows[position]

            holder.setLeadingIcon(null)
            holder.setPrimaryText(item.primary)
            holder.setSecondaryText(item.secondary)

            holder.itemView.setOnClickListener {
                when (item.id) {
                    AccountRowType.STRIPE -> onClickStripe()
                    AccountRowType.WECHAT -> onClickWechat()
                    AccountRowType.MOBILE -> {
                        // For mobile-created account, forbid user to update mobile to another one.
                        if (AccountCache.get()?.isMobileEmail == true) {
                            AlertDialogFragment
                                .newMsgInstance("手机号创建的账号不允许更改")
                                .show(childFragmentManager, "PreventModifyMobile")
                            return@setOnClickListener
                        }

                        UpdateActivity.start(requireContext(), item.id)
                    }
                    else -> UpdateActivity.start(requireContext(), item.id)
                }
            }
        }

        override fun getItemCount() = rows.size

        fun setData(items: List<AccountRow>) {
            this.rows = items
            notifyDataSetChanged()
        }

        // If user is already a Stripe customer, show the
        // CustomerActivity; otherwise pop up a dialog urging
        // user to become a Stripe customer.
        private fun onClickStripe() {
            val account = sessionManager.loadAccount() ?: return

            if (!account.stripeId.isNullOrBlank()) {
                CustomerActivity.start(context)
                return
            }

            AlertDialogFragment
                .newStripeCustomer(account.email)
                .onPositiveButtonClicked { dialog, _ ->
                    toast(R.string.stripe_init)
                    customerViewModel.createCustomer(account)
                    dialog.dismiss()
                }
                .onNegativeButtonClicked { dialog, _ ->
                    dialog.dismiss()
                }
                .show(childFragmentManager, "CreateStripeCustomer")
        }

        // If email-wechat linked, show wechat details;
        // otherwise show a dialog to ask user to perform wechat OAuth.
        private fun onClickWechat() {
            AccountCache.get()?.let {
                if (it.isLinked) {
                    WxInfoActivity.start(requireContext())
                    return
                }

                if (it.isFtcOnly) {
                    LinkWxDialogFragment().show(childFragmentManager, "EmailLinkWechat")
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                FtcAccountFragment()
    }
}


