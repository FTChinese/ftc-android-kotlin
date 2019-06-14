package com.ft.ftchinese.ui.account

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.LoginMethod
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.util.FileCache
import com.ft.ftchinese.util.RequestCode
import kotlinx.android.synthetic.main.fragment_wx_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast
import java.io.ByteArrayInputStream

/**
 * Contained both by [AccountActivity] and [WxInfoActivity]
 */
@kotlinx.coroutines.ExperimentalCoroutinesApi
class WxFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var cache: FileCache
    private lateinit var viewModel: AccountViewModel

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

        link_email_btn.setOnClickListener {
            LinkActivity.startForResult(activity, RequestCode.LINK)
        }

        unlink_btn.setOnClickListener {

        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val account = sessionManager.loadAccount() ?: return

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        viewModel.accountResult.observe(this, Observer {
            val accountResult = it ?: return@Observer

            viewModel.stopRefreshing()

            if (accountResult.error != null) {
                toast(accountResult.error)
                return@Observer
            }

            if (accountResult.exception != null) {
                activity?.handleException(accountResult.exception)
                return@Observer
            }

            if (accountResult.success == null) {
                toast("Unknown error")
                return@Observer
            }

            toast(R.string.prompt_updated)

            sessionManager.saveAccount(accountResult.success)

            if (!accountResult.success.isWxOnly) {
                viewModel.switchUI(LoginMethod.EMAIL)
            }
        })

        viewModel.avatarResult.observe(this, Observer {
            if (it.exception != null) {
                activity?.handleException(it.exception)
                return@Observer
            }

            val bytes = it.success ?: return@Observer

            wx_avatar.setImageDrawable(
                    Drawable.createFromStream(
                            ByteArrayInputStream(bytes),
                            account.wechat.avatarName
                    )
            )

            launch {
                withContext(Dispatchers.IO) {
                    cache.writeBinaryFile(account.wechat.avatarName, bytes)
                }
            }
        })
    }

    private fun initUI() {
        val account = sessionManager.loadAccount()

        if (account == null) {
            toast("Account not found")
            return
        }

        info("Show wechat acount: $account")

        if (account.wechat.isEmpty) {
            toast(R.string.wechat_not_found)
            return
        }

        // Use locally cached avatar first.
        // If not found, fetch it from network.
        launch {
            val drawable = withContext(Dispatchers.IO) {
                cache.readDrawable(account.wechat.avatarName)
            }

            if (drawable != null) {
                wx_avatar.setImageDrawable(drawable)
                return@launch
            }

            if (activity?.isNetworkConnected() != true) {
                return@launch
            }

            val imageUrl = account.wechat.avatarUrl ?: return@launch

            viewModel.downloadAvatar(imageUrl)
        }

        wx_nickname.text = account.wechat.nickname

        // Test if accounts if coupled to FTC account.
        // If true, do not show the instruction to bind accounts.
        if (!account.isLinked) {
            unlink_btn.visibility = View.GONE
            return
        }

        instruction_tv.visibility = View.GONE
        link_email_btn.visibility = View.GONE
    }


    companion object {
        @JvmStatic
        fun newInstance() = WxFragment()
    }
}
