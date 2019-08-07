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
import kotlinx.android.synthetic.main.fragment_update_username.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateNameFragment : ScopedFragment(), AnkoLogger {
    private lateinit var sessionManager: SessionManager
    private lateinit var updateViewModel: UpdateViewModel



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
        user_name_input.requestFocus()
        save_btn.isEnabled = false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        updateViewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(UpdateViewModel::class.java)
        } ?: throw Exception("Invalid Exception")


        setUp()
    }

    private fun setUp() {
        // Validate input data.
        updateViewModel.updateFormState.observe(this, Observer {
            val updateState = it ?: return@Observer

            save_btn.isEnabled = updateState.isDataValid

            if (updateState.nameError != null) {
                user_name_input.error = getString(updateState.nameError)
                user_name_input.requestFocus()
            }
        })

        user_name_input.afterTextChanged {
            updateViewModel.userNameDataChanged(
                    currentName = value_name_tv.text.toString(),
                    newName = user_name_input.text.toString().trim()
            )
        }

        save_btn.setOnClickListener {
            updateViewModel.userNameDataChanged(
                    currentName = value_name_tv.text.toString(),
                    newName = user_name_input.text.toString().trim()
            )

            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            updateViewModel.showProgress(true)
            enableInput(false)

            updateViewModel.updateUserName(
                    userId = userId,
                    name = user_name_input.text.toString().trim()
            )
        }

        // Re-enable button on error.
        updateViewModel.updateResult.observe(this, Observer {
            if (it.error != null || it.exception != null) {
                enableInput(false)
            }
        })
    }

    private fun enableInput(value: Boolean) {
        user_name_input.isEnabled = value
        save_btn.isEnabled = value
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdateNameFragment()
    }
}
