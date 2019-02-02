package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

/**
 * Show user's account details
 */
class AccountActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return AccountFragment.newInstance()
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

internal class AccountFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    private var sessionManager: SessionManager? = null

    private fun showProgress(value: Boolean) {
        mListener?.onProgress(value)
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

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val account = withContext(Dispatchers.IO) {
                    sessionManager?.loadAccount()?.refresh()
                }

                // hide refreshing indicator
                swipe_refresh.isRefreshing = false

                if (account != null) {
                    sessionManager?.saveAccount(account)
                    updateUI(account)
                }

                toast(R.string.success_updated)
            } catch (e: ClientError) {
                info(e.message)
                swipe_refresh.isRefreshing = false

                handleClientError(e)

            } catch (e: Exception) {
                e.printStackTrace()

                swipe_refresh.isRefreshing = false
                activity?.handleException(e)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            mListener = context
        }

        if (context != null) {
            sessionManager = SessionManager.getInstance(context)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView")
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set swipe refresh listener
        swipe_refresh.setOnRefreshListener(this)

        // Set event handlers.
        email_container.setOnClickListener {
            UpdateEmailActivity.start(context)
        }

        user_name_container.setOnClickListener{
            UpdateUserNameActivity.start(context)
        }

        password_container.setOnClickListener {
            UpdatePasswordActivity.start(context)
        }

        info("onViewCreated")

        val account = sessionManager?.loadAccount() ?: return

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

        if (account.userName != null) {
            user_name_text.text = account.userName
        }

        // Not bound to wechat
        if (account.unionId.isNullOrBlank()) {

        } else {
            // Already bound to wechat
            wechat_status.text = getString(R.string.status_bound)
        }
    }

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
        fun newInstance() = AccountFragment()
    }
}

