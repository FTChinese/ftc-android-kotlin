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
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.ui.data.FetchResult
import com.ft.ftchinese.ui.lists.TwoLineItemViewHolder
import com.ft.ftchinese.ui.wxlink.LinkWxDialogFragment
import com.ft.ftchinese.viewmodel.AccountViewModel
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FtcAccountFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var binding: FragmentFtcAccountBinding

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
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_updated)
                    sessionManager.saveAccount(it.data)

                    if (it.data.isWxOnly) {
                        accountViewModel.switchUI(LoginMethod.WECHAT)
                        return@observe
                    }
                    updateUI()
                }
            }
        }
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
            accountViewModel.refresh(acnt)
        }
    }

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
                    AccountRowType.STRIPE -> CustomerActivity.start(context)
                    // If email-wechat linked, show wechat details;
                    // otherwise show a dialog to ask user to perform wechat OAuth.
                    AccountRowType.WECHAT -> {
                        sessionManager.loadAccount()?.let {
                            if (it.isLinked) {
                                WxInfoActivity.start(requireContext())
                                return@setOnClickListener
                            }

                            if (it.isFtcOnly) {
                                LinkWxDialogFragment().show(childFragmentManager, "EmailLinkWechat")
                            }
                        }
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
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                FtcAccountFragment()
    }
}


