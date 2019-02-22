package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.WxOAuth
import com.ft.ftchinese.models.WxSessionManager
import com.ft.ftchinese.util.*
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_ftc_account.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class FtcAccountFragment : Fragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var job: Job? = null
    private var listener: OnAccountInteractionListener? = null

    private var sessionManager: SessionManager? = null
    private var wxSessManager: WxSessionManager? = null
    private var wxApi: IWXAPI? = null

    private fun showProgress(value: Boolean) {
        listener?.onProgress(value)
    }

    private fun allowInput(v: Boolean) {
        request_verify_button?.isEnabled = v
    }

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
    }

    /**
     * Refresh account data.
     * It is necessary to inform parent activity of data change?
     */
    override fun onRefresh() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            stopRefresh()

            return
        }

        toast(R.string.progress_refresh_account)

        val account = sessionManager?.loadAccount()
        info(account)

        if (account == null) {
            stopRefresh()
            return
        }

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val updatedAccount = withContext(Dispatchers.IO) {
                    account.refresh()
                }

                // hide refreshing indicator
                stopRefresh()

                if (updatedAccount == null) {
                    return@launch
                }

                info(updatedAccount)
                sessionManager?.saveAccount(updatedAccount)
                // If after refreshing, user account changed, e.g.
                // previously email and wechat is bound, somehow on
                // another platform the two account unbound.
                if (!updatedAccount.isWxOnly) {
                    listener?.onAccountUpdate()
                    return@launch
                }

                updateUI(updatedAccount)

                toast(R.string.success_updated)
            } catch (e: ClientError) {
                info(e)
                stopRefresh()

                handleClientError(e)

            } catch (e: Exception) {
                info(e)

                stopRefresh()
                activity?.handleException(e)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnAccountInteractionListener) {
            listener = context
        }

        if (context != null) {
            sessionManager = SessionManager.getInstance(context)
            wxSessManager = WxSessionManager.getInstance(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wxApi = WXAPIFactory.createWXAPI(context, BuildConfig.WX_SUBS_APPID)
        wxApi?.registerApp(BuildConfig.WX_SUBS_APPID)

        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView")
        return inflater.inflate(R.layout.fragment_ftc_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val account = sessionManager?.loadAccount() ?: return

        // Set swipe refresh listener
        swipe_refresh.setOnRefreshListener(this)

        // Set event handlers.
        email_container.setOnClickListener {
            UpdateAccountActivity.startForEmail(context)
        }

        user_name_container.setOnClickListener{
            UpdateAccountActivity.startForUserName(context)
        }

        password_container.setOnClickListener {
            UpdateAccountActivity.startForPassword(context)
        }


        wechat_container.setOnClickListener {
            if (account.isCoupled) {
                WxAccountActivity.start(context)

                return@setOnClickListener
            }

            if (account.isFtcOnly) {
                bindWechat()
                return@setOnClickListener
            }
        }

        info(account)

        updateUI(account)
    }

    override fun onResume() {
        super.onResume()

        info("onResume")
        val account = sessionManager?.loadAccount() ?: return
        updateUI(account)
    }

    private fun updateUI(account: Account) {

        if (account.isVerified) {
            verify_email_container.visibility = View.GONE
        } else {
            request_verify_button.setOnClickListener {
                requestVerification()
            }
        }

        if (account.email.isNotBlank()) {
            email_text.text = account.email
        }

        if (!account.userName.isNullOrBlank()) {
            user_name_text.text = account.userName
        }

        if (account.isCoupled) {
            wechat_bound_tv.text = getString(R.string.action_bound_account)
        }
    }

    /**
     * Launch Wechat OAuth workflow to request a code from wechat.
     * It will jump to wxapi.WXEntryActivity.
     */
    private fun bindWechat() {
        val stateCode = WxOAuth.stateCode()

        wxSessManager?.saveState(stateCode)

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = stateCode
    }

    /**
     * Resend verification email to user.
     */
    private fun requestVerification() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        showProgress(true)
        allowInput(false)

        toast(R.string.progress_request_verification)

        // If the account if not found, do nothing.
        // In the future, we might need to take into account user logged in via social platforms.
        val account = sessionManager?.loadAccount() ?: return

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val done = withContext(Dispatchers.IO) {
                    account.requestVerification()
                }

                // If request succeeds, disable request verification button.
                showProgress(false)
                allowInput(!done)

                toast(R.string.success_letter_sent)

            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                handleClientError(e)

            } catch (e: Exception) {
                e.printStackTrace()

                showProgress(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    private fun handleClientError(resp: ClientError) {
        when (resp.statusCode) {
            // If this account is not found. It's rare but possible. For example, user logged in at one place, then deleted account at another place.
            404 -> {
                toast(R.string.api_account_not_found)
            }
            // All other errors are treated as server error.
            else -> {
                activity?.handleApiError(resp)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        fun newInstance() = FtcAccountFragment()
    }
}