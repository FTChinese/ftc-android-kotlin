package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.models.UserNameUpdate
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.handleApiError
import com.ft.ftchinese.util.handleException
import com.ft.ftchinese.util.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_update_username.*
import kotlinx.coroutines.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

class UpdateNameFragment : Fragment(), AnkoLogger {

    private var job: Job? = null
    private var mListener: OnAccountInteractionListener? = null

    private var mSession: SessionManager? = null

    private var isInProgress: Boolean
        get() = !save_button.isEnabled
        set(value) {
            mListener?.onProgress(value)
        }

    private var isInputAllowed: Boolean
        get() = user_name.isEnabled
        set(value) {
            user_name?.isEnabled = value
            save_button?.isEnabled = value
        }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnAccountInteractionListener) {
            mListener = context
        }

        if (context != null) {
            mSession = SessionManager.getInstance(context)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_username, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        save_button.setOnClickListener {
            attemptSave()
        }

        current_name.text = mSession?.loadAccount()?.userName
    }

    private fun attemptSave() {
        user_name.error = null
        val userNameStr = user_name.text.toString()

        var cancel = false

        if (userNameStr.isBlank()) {
            user_name.error = getString(R.string.error_field_required)
            cancel = true
        } else if (userNameStr == mSession?.loadAccount()?.userName) {
            user_name.error = getString(R.string.error_name_unchanged)
            cancel = true
        }

        if (cancel) {
            user_name.requestFocus()
            return
        }

        save(userNameStr)
    }

    private fun save(userName: String) {

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        val uuid = mSession?.loadAccount()?.id ?: return

        isInProgress = true
        isInputAllowed = false

        job = GlobalScope.launch(Dispatchers.Main) {


            val userNameUpdate = UserNameUpdate(userName)

            try {
                info("Start updating mAccount userName")

                val done = withContext(Dispatchers.IO) {
                    userNameUpdate.send(uuid)
                }

                isInProgress = false

                if (done) {

                    mSession?.updateUserName(userName)

                    current_name.text = userName

                    toast(R.string.success_saved)
                } else {

                }
            } catch (e: ClientError) {
                isInProgress = false
                isInputAllowed = true

                activity?.handleApiError(e)

            } catch (e: Exception) {
                isInProgress = false
                isInputAllowed = true

                activity?.handleException(e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        job?.cancel()
    }

    companion object {

        fun newInstance() = UpdateNameFragment()
    }
}