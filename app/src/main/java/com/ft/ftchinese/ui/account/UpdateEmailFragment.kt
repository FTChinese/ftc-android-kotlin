package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.*
import com.ft.ftchinese.model.FtcUser
import com.ft.ftchinese.model.SessionManager
import com.ft.ftchinese.util.ClientError
import kotlinx.android.synthetic.main.fragment_update_email.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateEmailFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: UpdateViewModel

    private fun enableInput(value: Boolean) {
        email_input.isEnabled = value
        save_btn.isEnabled = value
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        sessionManager = SessionManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_update_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        value_email_tv.text = sessionManager.loadAccount()?.email
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        viewModel.updateFormState.observe(this, Observer {
            val updateState = it ?: return@Observer

            save_btn.isEnabled = updateState.isDataValid

            if (updateState.emailError != null) {
                save_btn.error = getString(updateState.emailError)
                email_input.requestFocus()
            }
        })

        viewModel.inputEnabled.observe(this, Observer {
            save_btn.isEnabled = it
        })

        email_input.afterTextChanged {
            viewModel.emailDataChanged(
                    currentEmail = value_email_tv.text.toString(),
                    newEmail = email_input.text.toString().trim()
            )
        }

        save_btn.setOnClickListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            it.isEnabled = false
            viewModel.showProgress(true)

            viewModel.updateEmail(
                    userId = userId,
                    email = email_input.text.toString().trim()
            )
        }
    }


    private fun save(emailStr: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)

            return
        }

        // If user id is not found, we could not perform updating.
        val uuid = sessionManager.loadAccount()?.id ?: return

        viewModel.showProgress(true)
        enableInput(false)

        launch {

            try {
                info("Start updating email")

                val done = withContext(Dispatchers.IO) {
                    FtcUser(uuid).updateEmail(emailStr)
                }

                viewModel.showProgress(false)

                if (done) {

                    toast(R.string.prompt_saved)

                    viewModel.setDone(true)
                } else {

                }
            } catch (e: ClientError) {
                viewModel.showProgress(false)
                enableInput(true)

                activity?.handleApiError(e)

            } catch (e: Exception) {
                viewModel.showProgress(false)
                enableInput(true)

                activity?.handleException(e)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdateEmailFragment()
    }
}
