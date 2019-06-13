package com.ft.ftchinese.ui.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedAppActivity
import com.ft.ftchinese.model.SessionManager
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxInfoActivity : ScopedAppActivity(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: AccountViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account)
        setSupportActionBar(toolbar)

        sessionManager = SessionManager.getInstance(this)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        supportFragmentManager.commit {
            replace(R.id.frag_account, WxFragment.newInstance())
        }

        viewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        viewModel.refreshing.observe(this, Observer {
            swipe_refresh.isRefreshing = it
        })

        swipe_refresh.setOnRefreshListener {
            
        }
    }

    companion object {
        fun start(context: Context?) {
            context?.startActivity(
                    Intent(context, WxInfoActivity::class.java)
            )
        }
    }
}
