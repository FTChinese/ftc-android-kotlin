package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.handleApiError
import com.ft.ftchinese.base.handleException
import com.ft.ftchinese.base.isNetworkConnected
import com.ft.ftchinese.models.FtcUser
import com.ft.ftchinese.models.SessionManager
import com.ft.ftchinese.user.Validator
import com.ft.ftchinese.util.ClientError
import kotlinx.android.synthetic.main.fragment_update_username.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateNameFragment : ScopedFragment(), AnkoLogger {
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: UpdateViewModel

    private fun allowInput(value: Boolean) {
        user_name_input.isEnabled = value
        save_btn.isEnabled = value
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_username, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        value_name_tv.text = sessionManager.loadAccount()?.userName
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

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

        val uuid = sessionManager.loadAccount()?.id ?: return

        viewModel.showProgress(true)
        allowInput(false)

        launch {

            try {
                info("Start updating userName")

                val done = withContext(Dispatchers.IO) {
                    FtcUser(uuid).updateUserName(userName)
                }

                viewModel.showProgress(false)

                if (done) {

                    toast(R.string.prompt_saved)

                    viewModel.setDone(true)
                } else {

                }
            } catch (e: ClientError) {
                info(e)
                viewModel.setDone(false)
                allowInput(true)

                activity?.handleApiError(e)

            } catch (e: Exception) {
                viewModel.setDone(false)
                allowInput(true)

                activity?.handleException(e)
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdateNameFragment()
    }
}
