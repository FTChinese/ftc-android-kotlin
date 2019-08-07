package com.ft.ftchinese.ui.account

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.*
import com.ft.ftchinese.model.SessionManager
import kotlinx.android.synthetic.main.fragment_update_email.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateEmailFragment : ScopedFragment(), AnkoLogger {

    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel
    private var currentEmail: String? = null

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

        currentEmail = sessionManager.loadAccount()?.email

        save_btn.isEnabled = false

        label_email_tv.text = getString(R.string.label_current_email, currentEmail ?: getString(R.string.prompt_not_set))
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")

        // Validate form data.
        // Note this only works if user actually entered something.
        // If user does not enter anything, and clicked save button,
        // empty string will be submitted directly.
        updateViewModel.updateFormState.observe(this, Observer {
            val updateState = it ?: return@Observer

            save_btn.isEnabled = updateState.isDataValid

            if (updateState.emailError != null) {
                email_input.error = getString(updateState.emailError)
                email_input.requestFocus()
            }
        })

        email_input.afterTextChanged {
            updateViewModel.emailDataChanged(
                    currentEmail = currentEmail ?: "",
                    newEmail = email_input.text.toString().trim()
            )
        }

        save_btn.setOnClickListener {
            updateViewModel.emailDataChanged(
                    currentEmail = currentEmail ?: "",
                    newEmail = email_input.text.toString().trim()
            )

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            enableInput(false)
            updateViewModel.showProgress(true)

            updateViewModel.updateEmail(
                    userId = userId,
                    email = email_input.text.toString().trim()
            )
        }

        updateViewModel.updateResult.observe(this, Observer {
            if (it.error != null || it.exception != null) {
                enableInput(true)
            }
        })
    }

    private fun enableInput(value: Boolean) {
        email_input.isEnabled = value
        save_btn.isEnabled = value
    }

    companion object {
        @JvmStatic
        fun newInstance() = UpdateEmailFragment()
    }
}
