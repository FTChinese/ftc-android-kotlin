package com.ft.ftchinese.ui.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.ft.ftchinese.R
import com.ft.ftchinese.base.ScopedFragment
import com.ft.ftchinese.base.isNetworkConnected
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = activity?.run {
            ViewModelProviders.of(this)
                    .get(LoginViewModel::class.java)
        } ?: throw Exception("Invalid Activity")

        viewModel.loginFormState.observe(this, Observer {
            val loginState = it ?: return@Observer

            next_btn.isEnabled = loginState.isDataValid

            if (loginState.emailError != null) {
                email_input.error = getString(loginState.emailError)
                email_input.requestFocus()
            }
        })

        viewModel.input.observe(this, Observer {
            next_btn.isEnabled = it
        })

        email_input.afterTextChanged {
            viewModel.emailDataChanged(email_input.text.toString().trim())
        }

        /**
         * Check whether email exists.
         */
        next_btn.setOnClickListener {
            if (activity?.isNetworkConnected() != true) {
                toast(R.string.prompt_no_network)
                return@setOnClickListener
            }

            it.isEnabled = false
            viewModel.checkEmail(email_input.text.toString().trim())
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailFragment()
    }
}


/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
