package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ft.ftchinese.BuildConfig
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.models.*
import com.ft.ftchinese.util.ClientError
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import kotlinx.android.synthetic.main.fragment_ftc_account.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class FtcFragment : ScopedFragment(),
        SwipeRefreshLayout.OnRefreshListener,
        AnkoLogger {

    private var sessionManager: SessionManager? = null
    private var wxApi: IWXAPI? = null
    lateinit var viewModel: AccountViewModel

    private fun allowInput(v: Boolean) {
        request_verify_button?.isEnabled = v
    }

    private fun stopRefresh() {
        swipe_refresh.isRefreshing = false
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

        val account = sessionManager?.loadAccount() ?: return

        swipe_refresh.setOnRefreshListener(this)

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

        updateUI(account)
    }

    override fun onResume() {
        super.onResume()

        val account = sessionManager?.loadAccount() ?: return

        updateUI(account)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(AccountViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

    }

    private fun updateUI(account: Account) {
        if (account.isVerified) {
            verify_email_container.visibility = View.GONE
        } else {
            request_verify_button.setOnClickListener {
                requestVerification()
            }
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

        wechat_bound_tv.text = if (account.isCoupled) {
            getString(R.string.action_bound_account)
        } else {
            getString(R.string.action_bind_account)
        }

        if (account.isCoupled) {
            wechat_container.setOnClickListener {
                WxInfoActivity.start(context)
            }
        } else if (account.isFtcOnly) {
            wechat_container.setOnClickListener {
                bindWechat()
            }
        }
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

        info("Starting refreshing account: $account")

        if (account == null) {
            stopRefresh()
            return
        }

        launch {
            try {
                val updatedAccount = withContext(Dispatchers.IO) {
                    account.refresh()
                }

                // hide refreshing indicator
                stopRefresh()

                if (updatedAccount == null) {
                    return@launch
                }

                info("Refreshed account: $updatedAccount")
                sessionManager?.saveAccount(updatedAccount)

                /**
                 * If after refreshing, user account changed, e.g.
                 * previously email and wechat is bound, somehow on
                 * another platform the two account unbound.
                 */
                if (updatedAccount.isWxOnly) {
                    info("A wechat only account. Switch UI.")
                    viewModel.changeLoginMethod(LoginMethod.WECHAT)
                    return@launch
                }

                updateUI(updatedAccount)

                toast(R.string.prompt_updated)
            } catch (e: ClientError) {
                info(e)
                stopRefresh()

                /**
                 * TODO logout current session if API responded 404.
                 * This is possible if user account is deleted on another platform.
                 */
                handleClientError(e)

            } catch (e: Exception) {
                info(e)

                stopRefresh()
                activity?.handleException(e)
            }
        }
    }

    /**
     * Launch Wechat OAuth workflow to request a code from wechat.
     * It will jump to wxapi.WXEntryActivity.
     */
    private fun bindWechat() {
        val stateCode = WxOAuth.stateCode()

        sessionManager?.saveWxState(stateCode)
        sessionManager?.saveWxIntent(WxOAuthIntent.BINDING)

        val req = SendAuth.Req()
        req.scope = WxOAuth.SCOPE
        req.state = stateCode

        wxApi?.sendReq(req)
    }

    /**
     * Resend verification email to user.
     */
    private fun requestVerification() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        viewModel.showProgress(true)
        allowInput(false)

        toast(R.string.progress_request_verification)

        // If the account if not found, do nothing.
        // In the future, we might need to take into account user logged in via social platforms.
        val account = sessionManager?.loadAccount() ?: return

        launch {
            try {
                val done = withContext(Dispatchers.IO) {
                    FtcUser(account.id).requestVerification()
                }

                // If request succeeds, disable request verification button.

                viewModel.showProgress(true)
                allowInput(!done)

                toast(R.string.prompt_letter_sent)

            } catch (e: ClientError) {
                viewModel.showProgress(true)
                allowInput(true)

                handleClientError(e)

            } catch (e: Exception) {
                e.printStackTrace()

                viewModel.showProgress(true)
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

    companion object {
        @JvmStatic
        fun newInstance() = FtcFragment()
    }
}
