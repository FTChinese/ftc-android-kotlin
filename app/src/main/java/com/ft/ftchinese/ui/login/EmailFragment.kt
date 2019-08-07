package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.ui.base.ScopedFragment
import com.ft.ftchinese.ui.base.afterTextChanged
import com.ft.ftchinese.ui.base.isNetworkConnected
import kotlinx.android.synthetic.main.fragment_email.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        return inflater.inflate(R.layout.fragment_email, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        email_input.requestFocus()
        next_btn.isEnabled = false
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")


        // Validate email upon changed.
        email_input.afterTextChanged {
            viewModel.emailDataChanged(email_input.text.toString().trim())
        }

        viewModel.loginFormState.observe(this, Observer {
            val loginState = it ?: return@Observer

            next_btn.isEnabled = loginState.isEmailValid

            if (loginState.error != null) {
                email_input.error = getString(loginState.error)
                email_input.requestFocus()
            }
        })

        next_btn.setOnClickListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            enableInput(false)
            viewModel.showProgress(true)

            viewModel.checkEmail(email_input.text.toString().trim())
        }

        // Enable or disable input depending on network result.
        // Only re-enable button if there's any error.
        viewModel.emailResult.observe(this, Observer {
            if (it.error != null || it.exception != null) {
                enableInput(true)
            }
        })
    }

    private fun enableInput(enable: Boolean) {
        email_input.isEnabled = enable
        next_btn.isEnabled = enable
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailFragment()
    }
}


