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

        val account = sessionManager.loadAccount()

        if (account == null) {
            toast("Account not found")
            return
        }

        binding.account = account
        // Enable swipe refresh for wechat-only account
        // since it is too complex to check whether
        // Wechat OAuth expired if the account is linked.
        binding.swipeRefresh.isEnabled = account.isWxOnly

        if (account.wechat.isEmpty) {
            toast(R.string.wechat_not_found)
            return
        }

        // Click on link or unlink button.
        /**
         * If current is not linked, launch [LinkFtcActivity] which will call setResult() to notify
         * [AccountActivity];
         * otherwise launch [UnlinkActivity]
         */
        binding.btnLinkOrUnlink.setOnClickListener {
            if (account.isLinked) {
                /**
                 * In this case this fragment is hosted inside [WxInfoActivity]
                 */
                UnlinkActivity.startForResult(activity)
            } else {
                /**
                 * In this case this fragment ins hosted inside [AccountActivity]
                 */
                LinkFtcActivity.startForResult(activity)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        accountViewModel = activity?.run {
            ViewModelProvider(this)
                    .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        // Check whether we could refresh wechat user info.
        // If refresh token is not expired, we'll start
        // refresh user's full account data.
        accountViewModel.wxRefreshResult.observe(viewLifecycleOwner, {
            onWxRefreshed(it)
        })

        // Refreshed account data.
        accountViewModel.accountRefreshed.observe(viewLifecycleOwner, {
            onAccountRefreshed(it)
        })

        // Avatar
        accountViewModel.avatarRetrieved.observe(viewLifecycleOwner, {
            onAvatarRetrieved(it)
        })

        binding.swipeRefresh.setOnRefreshListener {
            onRefresh()
        }

        val acnt = sessionManager.loadAccount() ?: return

        if (context?.isConnected != true) {
            toast(R.string.prompt_no_network)
            return
        }

        // Start fetching wechat avatar in background.
        accountViewModel.fetchWxAvatar(
                cache,
                acnt.wechat
        )
    }

    private fun onRefresh() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            toast("Account not found")
            binding.swipeRefresh.isEnabled = false
            return
        }

        if (context?.isConnected != true) {
            toast(R.string.prompt_no_network)
            binding.swipeRefresh.isEnabled = false
            return
        }

        val wxSession = sessionManager.loadWxSession()
        if (wxSession == null) {
            // If a linked user logged in, the WxSession
            // data will be definitely not existed.
            // In such case, show the re-authorization dialog.
            accountViewModel.showReAuth()
            binding.swipeRefresh.isEnabled = false
            return
        }

        toast(R.string.refreshing_wx_info)

        // Ask server to refresh wechat info.
        info("Start refreshing wx session")
        accountViewModel.refreshWxInfo(wxSession)
        // If we refresh avatar here, it won't use the
        // latest avatar.
        info("Start refreshing wechat avatar")
        accountViewModel.fetchWxAvatar(
                cache,
                account.wechat
        )

        toast(R.string.wait_while_refresh_wx)
    }

    private fun onWxRefreshed(result: FetchResult<WxRefreshState>) {
        info("Wechat info refresh finished")
        when (result) {
            is FetchResult.LocalizedError -> {
                binding.swipeRefresh.isEnabled = false
                toast(result.msgId)
            }
            is FetchResult.Error -> {
                binding.swipeRefresh.isEnabled = false
                result.exception.message?.let { toast(it) }
            }
            is FetchResult.Success -> {
                info("Wechat info refreshed successfully: ${result.data}")

                when (result.data) {
                    WxRefreshState.ReAuth -> {
                        binding.swipeRefresh.isEnabled = false
                    }
                    WxRefreshState.SUCCESS -> {
                        val acnt = sessionManager.loadAccount() ?: return

                        toast(R.string.refreshing_account)

                        accountViewModel.refresh(acnt)
                    }
                }
            }
        }
    }

    private fun onAccountRefreshed(result: FetchResult<Account>) {
        binding.swipeRefresh.isEnabled = false
        info("Account refresh finished")

        when (result) {
            is FetchResult.LocalizedError -> {
                toast(result.msgId)
            }
            is FetchResult.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is FetchResult.Success -> {
                toast(R.string.prompt_updated)

                sessionManager.saveAccount(result.data)

                if (!result.data.isWxOnly) {
                    info("Not an wechat-only account")
                    accountViewModel.switchUI(LoginMethod.EMAIL)
                }
            }
        }
    }

    private fun onAvatarRetrieved(result: FetchResult<InputStream>) {
        when (result) {
            is FetchResult.LocalizedError -> {
                toast(result.msgId)
            }
            is FetchResult.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is FetchResult.Success -> {
                binding.wxAvatar.setImageDrawable(
                        Drawable.createFromStream(
                                result.data,
                                WX_AVATAR_NAME
                        )
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = WxInfoFragment()
    }
}
