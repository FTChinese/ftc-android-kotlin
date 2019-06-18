package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_ftc_account.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FtcFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private var wxApi: IWXAPI? = null
    private lateinit var viewModel: AccountViewModel

    private fun stopRefreshing() {
        swipe_refresh.isRefreshing = false
    }

    private fun enableInput(v: Boolean) {
        request_verify_button?.isEnabled = v
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_ftc_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val account = sessionManager.loadAccount() ?: return

        // Set event handlers.
        email_container.setOnClickListener {
            UpdateActivity.startForEmail(context)
        }

        user_name_container.setOnClickListener{
            UpdateActivity.startForUserName(context)
        }

        password_container.setOnClickListener {
            UpdateActivity.startForPassword(context)
        }

        initUI(account)
    }

    override fun onResume() {
        super.onResume()

        val account = sessionManager.loadAccount() ?: return

        initUI(account)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Refreshed account.
        viewModel.accountResult.observe(this, Observer {
            val accountResult = it ?: return@Observer

            stopRefreshing()

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

            if (accountResult.success.isWxOnly) {
                info("A wechat only account. Switch UI.")
                viewModel.switchUI(LoginMethod.WECHAT)
                return@Observer
            }

            initUI(accountResult.success)
        })

        viewModel.sendEmailResult.observe(this, Observer {
            val sendEmailResult = it ?: return@Observer

            viewModel.showProgress(false)

            if (sendEmailResult.error != null) {
                enableInput(true)
                toast(sendEmailResult.error)
                return@Observer
            }

            if (sendEmailResult.exception != null) {
                enableInput(true)
                activity?.handleException(sendEmailResult.exception)
                return@Observer
            }

            if (sendEmailResult.success == true) {
                toast(R.string.prompt_letter_sent)
            } else {
                toast("Unknown error")
            }
        })

        swipe_refresh.setOnClickListener {


            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                stopRefreshing()
                return@setOnClickListener
            }

            val acnt = sessionManager.loadAccount()

            if (acnt == null) {
                toast("Account not found")
                stopRefreshing()
                return@setOnClickListener
            }

            toast(R.string.progress_refresh_account)

            viewModel.refresh(acnt)
        }

        request_verify_button.setOnClickListener {

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            viewModel.showProgress(true)
            enableInput(false)

            toast(R.string.progress_request_verification)

            viewModel.requestVerification(userId)
        }
    }

    private fun initUI(account: Account) {

        if (account.isVerified) {
            verify_email_container.visibility = View.GONE
        }

        email_text.text = if (account.email.isNotBlank()) {
            account.email
        } else {
            getString(R.string.prompt_not_set)
        }

        user_name_text.text = if (account.userName.isNullOrBlank()) {
            getString(R.string.prompt_not_set)
        } else {
            account.userName
        }

        wechat_bound_tv.text = if (account.isLinked) {
            getString(R.string.action_bound_account)
        } else {
            getString(R.string.action_bind_account)
        }

        if (account.isLinked) {
            wechat_container.setOnClickListener {
                WxInfoActivity.start(context)
            }
        } else if (account.isFtcOnly) {
            wechat_container.setOnClickListener {
                linkWechat()
            }
        }
    }

    /**
     * Launch Wechat OAuth workflow to request a code from wechat.
     * It will jump to wxapi.WXEntryActivity.
     */
    private fun linkWechat() {
        val stateCode = WxOAuth.stateCode()

        sessionManager.saveWxState(stateCode)
        sessionManager.saveWxIntent(WxOAuthIntent.LINK)

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = stateCode

        wxApi?.sendReq(req)
    }

    companion object {
        @JvmStatic
        fun newInstance() = FtcFragment()
    }
}
