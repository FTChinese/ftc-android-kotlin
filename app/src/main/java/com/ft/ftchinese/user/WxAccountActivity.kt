package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity;
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Wechat
import com.ft.ftchinese.models.WxManager
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.activity_wx_account.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.toast
import java.io.ByteArrayInputStream
import java.io.File

class WxAccountActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var sessionManager: SessionManager? = null
    private var wxManager: WxManager? = null
    private var cache: FileCache? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wx_account)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }
        swipe_refresh.setOnRefreshListener(this)

        sessionManager = SessionManager.getInstance(this)
        wxManager = WxManager.getInstance(this)
        cache = FileCache(this)

        updateUI()

        bind_email_btn.setOnClickListener {

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

        wx_nickname.text = getString(R.string.formatter_wx_name, account.wechat.nickname)
    }

    /**
     * This is similar to MainActivity.showAvatar.
     * Consider abstract them into one in the future.
     */
    private fun loadAvatar(wechat: Wechat) {
        // If the method is called by swipe refresh action,
        // omit local cache.
        if (!swipe_refresh.isRefreshing) {
            val drawable = cache?.readDrawable(wechat.avatarName)

            if (drawable != null) {
                wx_avatar.setImageDrawable(drawable)
            }
        }

        if (wechat.avatarUrl == null) {
            return
        }

        GlobalScope.launch(Dispatchers.Main) {
            val bytes = withContext(Dispatchers.IO) {
                wechat.downloadAvatar(filesDir)
            } ?: return@launch

            wx_avatar.setImageDrawable(
                    Drawable.createFromStream(
                            ByteArrayInputStream(bytes),
                            wechat.avatarName
                    )
            )
        }
    }

    override fun onRefresh() {
        if (!isNetworkConnected()) {
            toast(R.string.prompt_no_network)
            stopRefresh()

            return
        }

        toast(R.string.progress_refresh_account)

        val wxSession = wxManager?.loadSession() ?: return

        GlobalScope.launch(Dispatchers.Main) {
            val account = withContext(Dispatchers.IO) {
                wxSession.getAccount()
            }

            stopRefresh()

            if (account == null) {
                return@launch
            }

            sessionManager?.saveAccount(account)

            toast(R.string.success_updated)

            updateUI()
        }
    }

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WxAccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}
