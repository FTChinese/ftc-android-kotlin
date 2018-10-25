package com.ft.ftchinese.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.ft.ftchinese.R
import com.ft.ftchinese.models.ErrorResponse
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.Account
import com.ft.ftchinese.util.NextApi
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.gson
import kotlinx.android.synthetic.main.fragment_account.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

private const val VIEW_TYPE_SEPARATOR = 0x01 // Use list_item_separator.xml
private const val VIEW_TYPE_ITEM = 0x02 // Use list_item_table_row.xml
private const val VIEW_TYPE_TEXT = 0x03 // Use list_item_text_button.xml

/**
 * Show user's account details
 */
class AccountActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        val user = SessionManager.getInstance(this).loadUser()
        return AccountFragment.newInstance(user)
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, AccountActivity::class.java)
            context.startActivity(intent)
        }
    }
}

internal class AccountFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener, AnkoLogger {

    private var mUser: Account? = null
    private var job: Job? = null
    private var mListener: OnFragmentInteractionListener? = null

    private var isInProgress: Boolean = false
        set(value) {
            mListener?.onProgress(value)
        }

    /**
     * Update refresh retrieve mUser data from API, save it and update ui.
     */
    override fun onRefresh() {
        toast(R.string.action_updating)

        job = launch(UI) {
            try {
                val user = mUser?.refreshAsync()?.await()
                swipe_refresh.isRefreshing = false

                mUser = user
                updateUI()

                if (user != null) {
                    mListener?.onUserSession(user)
                }

                toast(R.string.success_updated)
            } catch (e: ErrorResponse) {
                when (e.statusCode) {
                    404 -> {
                        toast("用户不存在")
                    }
                }
            } catch (e: Exception) {
                handleException(e)
            } finally {
                swipe_refresh.isRefreshing = false
            }
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            mListener = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val userData = it.getString(ARG_USER_DATA)
            mUser = try {
                gson.fromJson<Account>(userData, Account::class.java)
            } catch (e: Exception) {
                null
            }
        }
        info("onCreate finished")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        info("onCreateView")
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        info("onViewCreated")
    }

    override fun onResume() {
        super.onResume()

        info("onResume")
        updateUI()
    }

    private fun updateUI() {
        info("updateUI")


    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {
        private const val ARG_USER_DATA = "user_data"
        fun newInstance(user: Account?) =
                AccountFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_USER_DATA, gson.toJson(user))
                    }
                }
    }


    private fun resendVerification() {
        val uuid = mUser?.id ?: return

        isInProgress = true

        job = launch(UI) {
            try {
                sendRequest(uuid)

                toast("邮件已发送")
            } catch (e: ErrorResponse) {
                isInProgress = false

                handleApiError(e)

            } catch (e: Exception) {
                isInProgress = false

                handleException(e)
            }
        }
    }

    private fun handleApiError(resp: ErrorResponse) {
        when (resp.statusCode) {
            403 -> {
                toast("Access Forbidden")
            }
            404 -> {
                toast("用户不存在")
            }
            500 -> {
                toast("服务器出错了")
            }
        }
    }

    private suspend fun sendRequest(uuid: String) {
        val job = async {
            Fetch()
                    .post(NextApi.REQUEST_VERIFICATION)
                    .noCache()
                    .setUserId(uuid)
                    .end()
        }

        val response = job.await()

        if (response.code() == 204) {
            info("Email sent")
        } else {
            info("Failed to updateAsync email. ${response.code()}: ${response.message()}")
        }
    }
}

