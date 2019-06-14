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
import com.ft.ftchinese.model.SessionManager
import kotlinx.android.synthetic.main.fragment_update_username.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class UpdateNameFragment : ScopedFragment(), AnkoLogger {
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: UpdateViewModel

    private fun enableInput(value: Boolean) {
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


        setUp()

        save_btn.setOnClickListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)

                return@setOnClickListener
            }

            val userId = sessionManager.loadAccount()?.id ?: return@setOnClickListener

            viewModel.showProgress(true)
            enableInput(false)

            viewModel.updateUserName(
                    userId = userId,
                    name = user_name_input.text.toString().trim()
            )
        }
    }

    private fun setUp() {
        // Validate input data.
        viewModel.updateFormState.observe(this, Observer {
            val updateState = it ?: return@Observer

            save_btn.isEnabled = updateState.isDataValid

            if (updateState.nameError != null) {
                save_btn.error = getString(updateState.nameError)
                user_name_input.requestFocus()
            }
        })

        // Re-enable save button in case of errors.
        viewModel.inputEnabled.observe(this, Observer {
            enableInput(it)
        })

        user_name_input.afterTextChanged {
            viewModel.userNameDataChanged(
                    currentName = value_name_tv.text.toString(),
                    newName = user_name_input.text.toString().trim()
            )
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = UpdateNameFragment()
    }
}
