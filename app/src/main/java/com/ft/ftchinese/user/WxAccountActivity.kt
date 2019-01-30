package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity;
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.WxManager
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

class WxAccountActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {


    private var sessionManager: SessionManager? = null
    private var wxManager: WxManager? = null

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

        wx_nickname.text = getString(R.string.formatter_wx_name, account.wechat.nickname)
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
