package com.ft.ftchinese.user

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.Wechat
import com.ft.ftchinese.util.*
import kotlinx.android.synthetic.main.fragment_wx_account.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.io.ByteArrayInputStream

/**
 * Used by both [WxAccountActivity] and [AccountActivity]
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxAccountFragment : ScopedFragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var listener: OnSwitchAccountListener? = null
    private var sessionManager: SessionManager? = null
//    private var wxSessManager: WxSessionManager? = null
    // Load drawable from filesDir.
    private var cache: FileCache? = null
//    private var job: Job? = null


    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
        cache = FileCache(context)

        if (context is OnSwitchAccountListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_wx_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        swipe_refresh.setOnRefreshListener(this)

        updateUI()

        // Handle click bind button
        link_email_btn.setOnClickListener {
            BindEmailActivity.startForResult(activity, RequestCode.LINK)
        }
    }

    /**
     * Handle result from BindEmailActivity.
     */
    private fun updateUI() {
        val account = sessionManager?.loadAccount()

        if (account == null) {
            toast("Account not found")
            return
        }

        info("Show wechat acount: $account")

        if (account.wechat.isEmpty) {
            toast(R.string.wechat_not_found)
            return
        }

        loadAvatar(account.wechat)

        wx_nickname.text = account.wechat.nickname

        // Test if accounts if coupled to FTC account.
        // If true, do not show the instruction to bind accounts.
        if (!account.isLinked) {
            return
        }

        instruction_tv.visibility = View.GONE
        link_email_btn.visibility = View.GONE
    }

    private fun loadAvatar(wechat: Wechat) {
//        if (!swipe_refresh.isRefreshing) {
//            val drawable = cache?.readDrawable(wechat.avatarName)
//
//            if (drawable != null) {
//                wx_avatar.setImageDrawable(drawable)
//                return
//            }
//        }

        if (wechat.avatarUrl == null) {
            return
        }

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        launch {
            val bytes = withContext(Dispatchers.IO) {
                wechat.downloadAvatar(context?.filesDir)
            } ?: return@launch

            wx_avatar.setImageDrawable(
                    Drawable.createFromStream(
                            ByteArrayInputStream(bytes),
                            wechat.avatarName
                    )
            )
        }
    }

    private fun stopRefresh() {
//        swipe_refresh.isRefreshing = false
    }
    /**
     * Refresh takes two steps:
     * 1. Ask API to refresh wechat userinfo;
     * 2. Fetch latest user account from API.
     */
    override fun onRefresh() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            stopRefresh()

            return
        }

        /**
         * If a user logged in with email account, the this account is bound to a wechat account,
         * WxSession data should not ever exist. If there is not WxSession data, you are not able to
         * access wechat oauth API.
         */
        launch {
            try {

                refreshInfo()

                toast(R.string.progress_fetching)

                val account = withContext(Dispatchers.IO) {
                    sessionManager?.loadAccount()?.refresh()
                }

                stopRefresh()

                if (account == null) {
                    return@launch
                }

                sessionManager?.saveAccount(account)

                info("Refreshed account: $account")

                /**
                 * Switch account fragment.
                 * This is only meaningful when hosted inside AccountActivity.
                 */
                if (account.isLinked && context is OnSwitchAccountListener) {
                    info("A bound account. Ask hosting activity to switch UI.")
                    listener?.onSwitchAccount()
                    return@launch
                }

                /**
                 * If after refreshing, we found out this wechat account is bound to an FTC account,
                 * ask hosting activity to switch fragment;
                 * otherwise simply update ui.
                 */
                toast(R.string.prompt_updated)

                updateUI()

            } catch (e: ClientError) {
                stopRefresh()
                info(e)

                activity?.handleApiError(e)
            } catch (e: Exception) {
                stopRefresh()
                info(e)

                activity?.handleException(e)
            }
        }
    }

    /**
     * This is an optional operation.
     */
    private suspend fun refreshInfo() {
        val wxSession = sessionManager?.loadWxSession() ?: return

        info("Wx session: $wxSession")

        toast(R.string.progress_updating)

        val refreshed = withContext(Dispatchers.IO) {
            wxSession.refreshInfo()
        }

        info("Refresh wechat info result: $refreshed")
    }

    companion object {
        fun newInstance() = WxAccountFragment()
    }
}
