package com.ft.ftchinese.ui.account

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.databinding.FragmentWxAccountBinding
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.WX_AVATAR_NAME
import com.ft.ftchinese.store.FileCache
import com.ft.ftchinese.store.SessionManager
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isConnected
import com.ft.ftchinese.ui.wxlink.LinkFtcActivity
import com.ft.ftchinese.ui.wxlink.UnlinkActivity
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.model.fetch.FetchResult
import com.ft.ftchinese.ui.login.WxExpireDialogFragment
import com.ft.ftchinese.viewmodel.WxRefreshState
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.io.InputStream

/**
 * Contained both by [AccountActivity] and [WxInfoActivity]
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxInfoFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var cache: FileCache
    private lateinit var accountViewModel: AccountViewModel
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

        // TODO: remove this
        accountViewModel = activity?.run {
            ViewModelProvider(this)
                .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        infoViewModel = activity?.run {
            ViewModelProvider(this)
                .get(WxInfoViewModel::class.java)
        } ?: throw Exception("Invalid exception")

        connectionLiveData.observe(viewLifecycleOwner) {
            infoViewModel.isNetworkAvailable.value = it
        }
        activity?.isConnected?.let {
            infoViewModel.isNetworkAvailable.value = it
        }

        binding.handler = this

        setupViewModel()
        initUI()
    }

    private fun setupViewModel() {
        infoViewModel.swipingLiveData.observe(viewLifecycleOwner) {
            binding.swipeRefresh.isRefreshing = it
        }

        infoViewModel.sessionState.observe(viewLifecycleOwner) {
            when (it) {
                WxRefreshState.SUCCESS -> {}
                WxRefreshState.ReAuth -> showReAuth()
            }
        }

        infoViewModel.accountLoaded.observe(viewLifecycleOwner) {
            when (it) {
                is FetchResult.LocalizedError -> toast(it.msgId)
                is FetchResult.Error -> it.exception.message?.let { msg -> toast(msg) }
                is FetchResult.Success -> {
                    toast(R.string.prompt_updated)
                    sessionManager.saveAccount(it.data)

                    if (!it.data.isWxOnly) {
                        info("Not an wechat-only account")
                        accountViewModel.switchUI(LoginMethod.EMAIL)
                    }
                }
            }
        }

        // Avatar
        accountViewModel.avatarRetrieved.observe(viewLifecycleOwner, {
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
        })

        // Set refreshing listener
        binding.swipeRefresh.setOnRefreshListener {
            val account = sessionManager.loadAccount()

            if (account == null) {
                toast("Account not found")
                infoViewModel.swipingLiveData.value = false
                return@setOnRefreshListener
            }

            val wxSession = sessionManager.loadWxSession()
            // If a linked user logged in, the WxSession
            // data will be definitely not existed.
            // In such case, show the re-authorization dialog.
            // TODO: how should we handle a linked user logged in with email?
            if (wxSession == null) {
                showReAuth()
                infoViewModel.swipingLiveData.value = false
                return@setOnRefreshListener
            }

            toast(R.string.refreshing_account)

            infoViewModel.refresh(account, wxSession)

            toast(R.string.wait_while_refresh_wx)
        }

        sessionManager.loadAccount()?.let {
            // Start fetching wechat avatar in background.
            accountViewModel.fetchWxAvatar(
                cache,
                it.wechat
            )
        }
    }

    private fun initUI() {

        sessionManager.loadAccount()?.let {
            binding.account = it

            // Enable swipe refresh for wechat-only account
            // since it is too complex to check whether
            // Wechat OAuth expired if the account is linked.
            binding.swipeRefresh.isEnabled = it.isWxOnly

            if (it.wechat.isEmpty) {
                toast(R.string.wechat_not_found)
                return
            }
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

    companion object {
        @JvmStatic
        fun newInstance() = WxInfoFragment()
    }
}
