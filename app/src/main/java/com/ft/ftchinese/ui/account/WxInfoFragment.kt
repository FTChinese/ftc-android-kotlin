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
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.showException
import com.ft.ftchinese.ui.base.isNetworkConnected
import com.ft.ftchinese.model.reader.LoginMethod
import com.ft.ftchinese.model.reader.SessionManager
import com.ft.ftchinese.model.reader.WX_AVATAR_NAME
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.viewmodel.AccountViewModel
import kotlinx.android.synthetic.main.fragment_wx_account.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

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
            val refreshResult = it ?: return@Observer

            if (refreshResult.error != null) {
                stopRefreshing()
                toast(refreshResult.error)

                return@Observer
            }

            if (refreshResult.exception != null) {
                stopRefreshing()
                activity?.showException(refreshResult.exception)

                return@Observer
            }

            if (refreshResult.isExpired) {
                stopRefreshing()
                // If the API tells us the refresh token
                // is expired, notify host activity to
                // show re-authorize dialog.
                accountViewModel.showReAuth()

                return@Observer
            }

            if (!refreshResult.success) {
                stopRefreshing()
                toast("Unknown error")

                return@Observer
            }

            val acnt = sessionManager.loadAccount() ?: return@Observer

            toast(R.string.refreshing_account)

            accountViewModel.refresh(acnt)
        })

        // Refreshed account data.
        accountViewModel.accountRefreshed.observe(viewLifecycleOwner, Observer {
            val accountResult = it ?: return@Observer

            stopRefreshing()

            if (accountResult.error != null) {
                toast(accountResult.error)
                return@Observer
            }

            if (accountResult.exception != null) {
                activity?.showException(accountResult.exception)
                return@Observer
            }

            if (accountResult.success == null) {
                toast("Unknown error")
                return@Observer
            }

            toast(R.string.prompt_updated)

            sessionManager.saveAccount(accountResult.success)

            if (!accountResult.success.isWxOnly) {
                accountViewModel.switchUI(LoginMethod.EMAIL)
            }
        })

        // Avatar
        accountViewModel.avatarRetrieved.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                return@Observer
            }

            if (it.exception != null) {
                activity?.showException(it.exception)
                return@Observer
            }

            if (it.success == null) {
                return@Observer
            }

            wx_avatar.setImageDrawable(
                    Drawable.createFromStream(
                            it.success,
                            WX_AVATAR_NAME
                    )
            )
        })

        swipe_refresh.setOnRefreshListener {
            onRefresh()
        }

        val acnt = sessionManager.loadAccount() ?: return

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

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

        toast(R.string.refreshing_account)

        accountViewModel.refreshWxInfo(wxSession)
        accountViewModel.fetchWxAvatar(
                cache,
                account.wechat
        )
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

    companion object {
        @JvmStatic
        fun newInstance() = WxInfoFragment()
    }
}
