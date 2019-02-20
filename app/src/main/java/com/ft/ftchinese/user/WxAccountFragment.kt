package com.ft.ftchinese.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Wechat
import com.ft.ftchinese.models.WxSessionManager
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.util.RequestCode
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_wx_account.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.io.ByteArrayInputStream

/**
 * Used by both WxAccountActivity and AccountActivity.
 */
class WxAccountFragment : Fragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var listener: OnAccountInteractionListener? = null
    private var sessionManager: SessionManager? = null
    private var wxSessManager: WxSessionManager? = null
    // Load drawable from filesDir.
    private var cache: FileCache? = null
    private var job: Job? = null


    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnAccountInteractionListener) {
            listener = context
        }

        if (context != null) {
            sessionManager = SessionManager.getInstance(context)
            wxSessManager = WxSessionManager.getInstance(context)
            cache = FileCache(context)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_wx_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipe_refresh.setOnRefreshListener(this)

        updateUI()

        bind_email_btn.setOnClickListener {
            BindFtcActivity.startForResult(activity, RequestCode.BOUND)
        }
    }

    private fun updateUI() {
        val account = sessionManager?.loadAccount()

        if (account == null) {
            toast("Account not found")
            return
        }

        info(account)


        loadAvatar(account.wechat)

        wx_nickname.text = account.wechat.nickname

        // Test if accounts if coupled to FTC account.
        // If true, do not show the instruction to bind accounts.
        if (!account.isCoupled) {
            return
        }

        instruction_tv.visibility = View.GONE
        bind_email_btn.visibility = View.GONE
    }

    private fun loadAvatar(wechat: Wechat) {
        if (!swipe_refresh.isRefreshing) {
            val drawable = cache?.readDrawable(wechat.avatarName)

            if (drawable != null) {
                wx_avatar.setImageDrawable(drawable)
                return
            }
        }

        if (wechat.avatarUrl == null) {
            return
        }

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        job = GlobalScope.launch(Dispatchers.Main) {
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

    /**
     * Refresh takes two steps:
     * 1. Ask API to refresh wechat userinfo;
     * 2. Fetch latest user account from API.
     */
    override fun onRefresh() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            swipe_refresh.isRefreshing = false

            return
        }

        toast(R.string.progress_refresh_account)

        val wxSession = wxSessManager?.loadSession() ?: return

        job = GlobalScope.launch(Dispatchers.Main) {
            val account = withContext(Dispatchers.IO) {
                wxSession.fetchAccount()
            }

            swipe_refresh.isRefreshing = false

            if (account == null) {
                return@launch
            }

            sessionManager?.saveAccount(account)

            toast(R.string.success_updated)

            updateUI()
        }
    }


    /**
     * After accounts bound, update based on latest account data.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != RequestCode.BOUND) {
            return
        }

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        listener?.onAccountUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun newInstance() = WxAccountFragment()
    }
}