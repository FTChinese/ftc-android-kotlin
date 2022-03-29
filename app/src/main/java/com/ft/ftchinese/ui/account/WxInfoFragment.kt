package com.ft.ftchinese.ui.account

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWxAccountBinding
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.WX_AVATAR_NAME
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.dialog.WxExpireDialogFragment
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import com.ft.ftchinese.ui.wxlink.UnlinkActivity
import org.jetbrains.anko.support.v4.toast

/**
 * Contained both by [AccountActivity] and [WxInfoActivity]
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxInfoFragment : ScopedFragment(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var cache: FileCache
    private lateinit var binding: FragmentWxAccountBinding

    private lateinit var infoViewModel: WxInfoViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        cache = FileCache(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_wx_account,
            container,
            false,
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        infoViewModel = activity?.run {
            ViewModelProvider(this)
                .get(WxInfoViewModel::class.java)
        } ?: throw Exception("Invalid exception")

        binding.handler = this
        binding.viewModel = infoViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        infoViewModel.progressLiveData.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }

        infoViewModel.sessionState.observe(viewLifecycleOwner) {
            when (it) {
                WxRefreshState.SUCCESS -> {}
                WxRefreshState.ReAuth -> showReAuth()
                else -> {}
            }
        }

        infoViewModel.accountLoaded.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> onAccountRefreshed(it.data)
            }
        }

        // Avatar
        infoViewModel.avatarLoaded.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error ->  it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    binding.wxAvatar.setImageDrawable(
                        Drawable.createFromStream(
                            it.data,
                            WX_AVATAR_NAME
                        )
                    )
                }
            }
        }
    }

    // Since we only allow refreshing for wechat-logged in user,
    // there is not need to worry about UI switching to FtcAccountFragment.
    private fun onAccountRefreshed(account: Account) {
        toast(R.string.refresh_success)

        sessionManager.saveAccount(account)
        infoViewModel.refreshAvatar(account.wechat, cache)

        // When user is logged in with Wechat, it must be a wechat-only account.
        // After rereshing, the account linking status could only have 2 case:
        // It is kept intact, so we only need to update the the ui data;
        // It is linked to an email account (possibly on other platforms). In such case wechat info is still kept, so we only show
        // the change info without switching to AccountActivity.
        // If is impossible for a wechat-only user to become
        // an email-only user.
        initUI()
    }

    private fun initUI() {

        sessionManager.loadAccount()?.let {
            binding.account = it

            // Enable swipe refresh for wechat-only account
            // since it is too complex to check whether
            // Wechat OAuth expired if the account is linked.
            binding.swipeRefresh.isEnabled = it.isWxOnly

            // Set refreshing listener
            binding.swipeRefresh.setOnRefreshListener(this)

            // Initial loading avatar.
            // Start fetching wechat avatar in background.
            infoViewModel.loadAvatar(
                it.wechat,
                cache,
            )
        }
    }

    private fun showReAuth() {
        WxExpireDialogFragment()
            .show(childFragmentManager, "WxExpireDialog")

        sessionManager.logout()
    }

    /**
     * If current is not linked, launch [LinkFtcActivity] which will call setResult() to notify
     * [AccountActivity];
     * otherwise launch [UnlinkActivity]
     */
    fun onClickLinkUnlink(view: View) {
        sessionManager.loadAccount()?.let {
            if (it.isLinked) {
                // this fragment is hosted inside [WxInfoActivity]
                UnlinkActivity.startForResult(activity)
            } else {
                // this fragment ins hosted inside [AccountActivity]
                LinkFtcActivity.startForResult(activity)
            }
        }
    }

    override fun onRefresh() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            toast(R.string.account_not_found)
            infoViewModel.progressLiveData.value = false
            return
        }

        val wxSession = sessionManager.loadWxSession()
        // If a linked user logged in, the WxSession
        // data will be definitely not existed.
        // In such case, show the re-authorization dialog.
        if (wxSession == null) {
            showReAuth()
            infoViewModel.progressLiveData.value = false
            return
        }

        toast(R.string.refreshing_account)

        infoViewModel.refresh(account, wxSession)

        toast(R.string.wait_while_refresh_wx)
    }

    companion object {
        @JvmStatic
        fun newInstance() = WxInfoFragment()
    }
}
