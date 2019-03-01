package com.ft.ftchinese.user

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ft.ftchinese.R
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.SessionManager
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
    private var listener: OnUpdateAccountListener? = null

    private var sessionManager: SessionManager? = null

    private fun showProgress(show: Boolean) {
        listener?.onProgress(show)
    }

    private fun allowInput(value: Boolean) {
        user_name_input.isEnabled = value
        save_btn.isEnabled = value
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)

        if (context is OnUpdateAccountListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_username, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        value_name_tv.text = sessionManager?.loadAccount()?.userName

        save_btn.setOnClickListener {

            val userName = user_name_input.text.toString().trim()

            val isValid = isNameValid(userName)

            if (!isValid) {
                return@setOnClickListener
            }

            save(userName)
        }
    }

    private fun isNameValid(userName: String): Boolean {
        user_name_input.error = null

        val msgId = Validator.ensureUserName(userName)
        if (msgId != null) {
            user_name_input.error = getString(msgId)
            user_name_input.requestFocus()

            return false
        }

        val currentName = value_name_tv.text.toString()

        if (currentName == userName) {
            user_name_input.error = getString(R.string.error_name_unchanged)
            user_name_input.requestFocus()

            return false
        }

        return true
    }

    private fun save(userName: String) {

        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        val uuid = sessionManager?.loadAccount()?.id ?: return

        showProgress(true)
        allowInput(false)

        job = GlobalScope.launch(Dispatchers.Main) {

            try {
                info("Start updating userName")

                val done = withContext(Dispatchers.IO) {
                    FtcUser(uuid).updateUserName(userName)
                }

                showProgress(false)

                if (done) {

                    toast(R.string.prompt_saved)

                    listener?.onUpdateAccount()
                } else {

                }
            } catch (e: ClientError) {
                showProgress(false)
                allowInput(true)

                activity?.handleApiError(e)

            } catch (e: Exception) {
                showProgress(false)
                allowInput(true)

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