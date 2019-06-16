package com.ft.ftchinese.ui.account

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.ft.ftchinese.R
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.Account
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.model.UnlinkAnchor
import com.ft.ftchinese.user.RowAdapter
import com.ft.ftchinese.user.TableRow
import kotlinx.android.synthetic.main.activity_unlink.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.simple_toolbar.*
import org.jetbrains.anko.toast

class UnlinkActivity : AppCompatActivity() {

    private lateinit var viewModel: AccountViewModel
    private lateinit var sessionManager: SessionManager
    private lateinit var linkViewModel: LinkViewModel
    private var anchor: UnlinkAnchor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlink)
        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        sessionManager = SessionManager.getInstance(this)

        viewModel = ViewModelProviders.of(this)
                .get(AccountViewModel::class.java)

        linkViewModel = ViewModelProviders.of(this)
                .get(LinkViewModel::class.java)

        initUI()

        linkViewModel.anchorSelected.observe(this, Observer {
                anchor = it
        })

        linkViewModel.unlinkResult.observe(this, Observer {
            progress_bar.visibility = View.GONE

            val unlinkResult = it ?: return@Observer
            if (unlinkResult.error != null) {
                toast(unlinkResult.error)
                unlink_button.isEnabled = true
                return@Observer
            }

            if (unlinkResult.exception != null) {
                handleException(unlinkResult.exception)
                unlink_button.isEnabled = true
                return@Observer
            }

            if (!unlinkResult.success) {
                toast("Unknown error occurred")
                unlink_button.isEnabled = true
                return@Observer
            }

            toast("Unlinked")

            // TODO: refresh account.
            finish()
        })

        unlink_button.setOnClickListener {
            if (!isNetworkConnected()) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            if (account.isMember && anchor == null) {
                toast(R.string.api_anchor_missing)
                return@setOnClickListener
            }

            progress_bar.visibility = View.VISIBLE
            unlink_button.isEnabled = false

            linkViewModel.unlink(account, anchor)
        }
    }

    private fun initUI() {
        val account = sessionManager.loadAccount() ?: return
        account_rv.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@UnlinkActivity)
            adapter = RowAdapter(buildRows(account))
        }

        if (account.isMember) {
            supportFragmentManager.commit {
                replace(R.id.frag_unlink_anchor, UnlinkAnchorFragment.newInstance())
            }
        }
    }

    private fun buildRows(account: Account): Array<TableRow> {
        return arrayOf(
                TableRow(
                        header = getString(R.string.label_ftc_account),
                        data = account.email
                ),
                TableRow(
                        header = getString(R.string.label_wx_account),
                        data = account.wechat.nickname ?: ""
                )
        )
    }

    companion object {
        fun startForResult(activity: Activity?, requestCode: Int) {
            activity?.startActivityForResult(
                    Intent(activity, UnlinkActivity::class.java),
                    requestCode
            )
        }
    }
}


