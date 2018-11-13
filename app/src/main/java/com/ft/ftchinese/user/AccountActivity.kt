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
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Account
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

    private var mAccount: Account? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    private var mSession: SessionManager? = null

    private var isInProgress: Boolean = false
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean = true
        set(value) {
            request_verify_button.isEnabled = value
        }

    /**
     * Refresh account data.
     * It is necessary to inform parent activity of data change?
     */
    override fun onRefresh() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        toast(R.string.progress_refresh_account)

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val account = mAccount?.refresh()

                // hide refreshing indicator
                swipe_refresh.isRefreshing = false

                mAccount = account


                if (account != null) {
                    mSession?.saveUser(account)
                    updateUI(account)
                }

                toast(R.string.success_updated)
            } catch (e: ErrorResponse) {
                info(e.message)
                swipe_refresh.isRefreshing = false

                handleApiError(e)

            } catch (e: Exception) {
                e.printStackTrace()

                swipe_refresh.isRefreshing = false
                handleException(e)
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            mListener = context
        }

        if (context != null) {
            mSession = SessionManager.getInstance(context)
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

        val account = mAccount ?: return

        updateUI(account)
    }

    override fun onResume() {
        super.onResume()

        info("onResume")
        val account = mAccount ?: return
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

        if (account.userName.isNotBlank()) {
            user_name_text.text = account.userName
        }

    }

    private fun requestVerification() {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        isInProgress = true
        isInputAllowed = false

        toast(R.string.progress_request_verification)

        // If the account if not found, do nothing.
        // In the future, we might need to take into account user logged in via social platforms.
        val account = mAccount ?: return

        job = GlobalScope.launch(Dispatchers.Main) {
            try {
                val statusCode = account.requestVerification()

                // If request succeeds, disable request verification button.
                isInProgress = false

                if (statusCode == 204) {
                    toast(R.string.success_letter_sent)

                } else {
                    toast("API response status: $statusCode")
                }

            } catch (ex: ErrorResponse) {
                info("API error response: $ex")

                isInProgress = false
                isInputAllowed = true

                handleErrorResponse(ex)

            } catch (e: Exception) {
                e.printStackTrace()

                isInProgress = false
                isInputAllowed = true

                handleException(e)
            }
        }
    }

    private fun handleErrorResponse(resp: ErrorResponse) {
        when (resp.statusCode) {
            // If request header does not contain X-User-Id
            401 -> {
                toast(R.string.api_unauthorized)
            }
            // If this account is not found. It's rare but possible. For example, use logged in at one place, then deleted account at another place.
            404 -> {
                toast(R.string.api_account_not_found)
            }
            // All other errors are treated as server error.
            else -> {
                handleApiError(resp)
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

