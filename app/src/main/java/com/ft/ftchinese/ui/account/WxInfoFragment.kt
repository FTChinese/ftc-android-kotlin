package com.ft.ftchinese.ui.account

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.ft.ftchinese.R
import com.ft.ftchinese.model.reader.Account
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WX_AVATAR_NAME
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.viewmodel.AccountViewModel
import com.ft.ftchinese.viewmodel.Result
import com.ft.ftchinese.viewmodel.WxRefreshState
import kotlinx.android.synthetic.main.fragment_wx_account.*
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

    private fun stopRefreshing() {
        swipe_refresh.isRefreshing = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        cache = FileCache(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_wx_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    private fun initUI() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            toast("Account not found")
            return
        }

        info("Show wechat acount: $account")

        // Enable swipe refresh for wechat-only account
        // since it is too complex to check whether
        // Wechat OAuth expired if the account is linked.
        swipe_refresh.isEnabled = account.isWxOnly

        if (account.wechat.isEmpty) {
            toast(R.string.wechat_not_found)
            return
        }

        wx_nickname.text = account.wechat.nickname
        // Test if accounts if coupled to FTC account.
        // If true, do not show the instruction to bind accounts.
        if (account.isLinked) {
            tv_urge_linking.visibility = View.GONE

            btn_link_or_unlink.text = getString(R.string.btn_unlink)
            btn_link_or_unlink.setOnClickListener {
                UnlinkActivity.startForResult(activity)
            }
        } else {
            btn_link_or_unlink.text = getString(R.string.btn_link)
            btn_link_or_unlink.setOnClickListener {
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
        accountViewModel.wxRefreshResult.observe(viewLifecycleOwner, Observer {
            onWxRefreshed(it)
        })

        // Refreshed account data.
        accountViewModel.accountRefreshed.observe(viewLifecycleOwner, Observer {
            onAccountRefreshed(it)
        })

        // Avatar
        accountViewModel.avatarRetrieved.observe(viewLifecycleOwner, Observer {
            onAvatarRetrieved(it)
        })

        swipe_refresh.setOnRefreshListener {
            onRefresh()
        }

        val acnt = sessionManager.loadAccount() ?: return

        if (activity?.isNetworkConnected() != true) {
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
            stopRefreshing()
            return
        }

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            stopRefreshing()
            return
        }

        val wxSession = sessionManager.loadWxSession()
        if (wxSession == null) {
            // If a linked user logged in, the WxSession
            // data will be definitely not existed.
            // In such case, show the re-authorization dialog.
            accountViewModel.showReAuth()
            stopRefreshing()
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

    private fun onWxRefreshed(result: Result<WxRefreshState>) {
        info("Wechat info refresh finished")
        when (result) {
            is Result.LocalizedError -> {
                stopRefreshing()
                toast(result.msgId)
            }
            is Result.Error -> {
                stopRefreshing()
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                info("Wechat info refreshed successfully: ${result.data}")

                when (result.data) {
                    WxRefreshState.ReAuth -> {
                        stopRefreshing()
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

    private fun onAccountRefreshed(result: Result<Account>) {
        stopRefreshing()

        info("Account refresh finished")

        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                toast(R.string.prompt_updated)

                sessionManager.saveAccount(result.data)

                if (!result.data.isWxOnly) {
                    info("Not an wechat-only account")
                    accountViewModel.switchUI(LoginMethod.EMAIL)
                }
            }
        }
    }

    private fun onAvatarRetrieved(result: Result<InputStream>) {
        when (result) {
            is Result.LocalizedError -> {
                toast(result.msgId)
            }
            is Result.Error -> {
                result.exception.message?.let { toast(it) }
            }
            is Result.Success -> {
                wx_avatar.setImageDrawable(
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
