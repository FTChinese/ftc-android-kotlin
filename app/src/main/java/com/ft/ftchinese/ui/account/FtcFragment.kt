package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.model.*
import com.ft.ftchinese.ui.StringResult
import com.ft.ftchinese.ui.login.AccountResult
import com.ft.ftchinese.util.RequestCode
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.account_row.view.*
import kotlinx.android.synthetic.main.activity_unlink.*
import kotlinx.android.synthetic.main.fragment_ftc_account.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FtcFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private var wxApi: IWXAPI? = null
    private lateinit var accountViewModel: AccountViewModel
    private var adapter: Adapter? = null

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

        val layout = LinearLayoutManager(context)
        adapter = Adapter(buildRows())

        account_rv.apply {
            setHasFixedSize(true)
            layoutManager = layout
            adapter = adapter
        }

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

        wallet_container.setOnClickListener {
            val account = sessionManager.loadAccount() ?: return@setOnClickListener

            if (account.stripeId == null) {
                if (activity?.isNetworkConnected() != true) {
                    toast(R.string.prompt_no_network)
                    return@setOnClickListener
                }
                accountViewModel.createCustomer(account)
            } else {
                CustomerActivity.start(context)
            }
        }

        initUI()
    }

    override fun onResume() {
        super.onResume()

        initUI()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        accountViewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        // Refreshed account.
        accountViewModel.accountRefreshed.observe(this, Observer {
            onAccountRefreshed(it)
        })

        accountViewModel.sendEmailResult.observe(this, Observer {
            onEmailSent(it)
        })

        accountViewModel.customerIdResult.observe(this, Observer {
            onCustomerCreated(it)
        })

        // Handle refresh action.
        swipe_refresh.setOnRefreshListener {
            info("Start refreshing")

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                stopRefreshing()
                return@setOnRefreshListener
            }

            val acnt = sessionManager.loadAccount()

            if (acnt == null) {
                toast("Account not found")
                stopRefreshing()
                return@setOnRefreshListener
            }

            toast(R.string.progress_refresh_account)

            accountViewModel.refresh(acnt)
        }

        request_verify_button.setOnClickListener {

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            accountViewModel.showProgress(true)
            enableInput(false)

            toast(R.string.progress_request_verification)

            accountViewModel.requestVerification(userId)
        }
    }

    private fun onAccountRefreshed(accountResult: AccountResult?) {
        stopRefreshing()

        if (accountResult == null) {
            return
        }

        if (accountResult.error != null) {
            toast(accountResult.error)
            return
        }

        if (accountResult.exception != null) {
            activity?.handleException(accountResult.exception)
            return
        }

        if (accountResult.success == null) {
            toast("Unknown error")
            return
        }

        toast(R.string.prompt_updated)

        sessionManager.saveAccount(accountResult.success)

        if (accountResult.success.isWxOnly) {
            info("A wechat only account. Switch UI.")
            accountViewModel.switchUI(LoginMethod.WECHAT)
            return
        }

        initUI()
    }

    private fun onEmailSent(result: BinaryResult?) {
        if (result == null) {
            return
        }

        accountViewModel.showProgress(false)

        if (result.error != null) {
            enableInput(true)
            toast(result.error)
            return
        }

        if (result.exception != null) {
            enableInput(true)
            activity?.handleException(result.exception)
            return
        }

        if (result.success) {
            toast(R.string.prompt_letter_sent)
        } else {
            toast("Unknown error")
        }
    }

    private fun onCustomerCreated(result: StringResult?) {
        accountViewModel.showProgress(false)
        if (result == null) {
            return
        }

        if (result.error != null) {
            toast(result.error)
            return
        }

        if (result.exception != null) {
            activity?.handleException(result.exception)
            return
        }

        val id = result.success ?: return
        sessionManager.saveStripeId(id)

        CustomerActivity.start(context)
    }

    private fun initUI() {

        val account = sessionManager.loadAccount() ?: return

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
                // Start for result in case use clicked
                // unlink button.
                WxInfoActivity.startForResult(activity, RequestCode.UNLINK)
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

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val labelView: TextView = view.tv_start
        val valueView: TextView = view.tv_end
    }

    class Adapter(
            private var items: Array<Item>
    ) : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.account_row, parent, false)

            return ViewHolder(view)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.labelView.text = item.label
            holder.valueView.text = item.value
        }
    }

    data class Item(
            val label: String,
            val value: String
    )

    private fun buildRows(): Array<Item> {
        val account = sessionManager.loadAccount() ?: return arrayOf()

        return arrayOf(
                Item(
                        label = "Email",
                        value = if (account.email.isNotBlank()) {
                            account.email
                        } else {
                            getString(R.string.prompt_not_set)
                        }
                ),
                Item(
                        label = "User Name",
                        value = if (account.userName.isNullOrBlank()) {
                            getString(R.string.prompt_not_set)
                        } else {
                            account.userName
                        }
                ),
                Item(
                        label = "Password",
                        value = ""
                ),
                Item(
                        label = "Wechat",
                        value = if (account.isLinked) {
                            getString(R.string.action_bound_account)
                        } else {
                            getString(R.string.action_bind_account)
                        }
                )
        )
    }

    companion object {
        @JvmStatic
        fun newInstance() = FtcFragment()
    }
}
