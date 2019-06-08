package com.ft.ftchinese.ui.login

import android.net.Uri
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
import com.ft.ftchinese.user.Validator
import com.ft.ftchinese.util.ClientError
import com.ft.ftchinese.util.Fetch
import com.ft.ftchinese.util.NextApi
import kotlinx.android.synthetic.main.fragment_email.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import org.jetbrains.anko.support.v4.toast

@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailFragment : ScopedFragment(),
        AnkoLogger {

    private lateinit var viewModel: LoginViewModel

    private fun enableInput(enable: Boolean) {
        email_input.isEnabled = enable
        next_btn.isEnabled = enable
    }

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

        /**
         * Check whether email exists.
         */
        next_btn.setOnClickListener {
            val email = email_input.text.toString().trim()
            val isValid = isEmailValid(email)
            if (!isValid) {
                return@setOnClickListener
            }

            emailExists(email)
        }
    }

    /**
     * Validate email. Returns true if it is valid; otherwise false.
     */
    private fun isEmailValid(email: String): Boolean {
        email_input.error = null

        val msgId = Validator.ensureEmail(email)

        if (msgId != null) {
            email_input.error = getString(msgId)
            email_input.requestFocus()

            return false
        }

        return true
    }


    /**
     * Ask API whether the mail exists.
     */
    private fun emailExists(email: String) {
        if (activity?.isNetworkConnected() != true) {
            toast(R.string.prompt_no_network)
            return
        }

        viewModel.showProgress(true)
        enableInput(false)

        val apiUrl = Uri.parse(NextApi.EMAIL_EXISTS)
                .buildUpon()
                .appendQueryParameter("k", "email")
                .appendQueryParameter("v", email)

        launch {

            try {
                val exists = withContext(Dispatchers.IO) {
                    val (resp, _) = Fetch()
                            .get(apiUrl.toString())
                            .responseApi()

                    resp.code() == 204
                }

                viewModel.showProgress(false)

                if (!exists) {
                    toast("Unknown error encountered")
                    return@launch
                }


                viewModel.foundEmail(Pair(email, true))

            } catch (e: ClientError) {

                viewModel.showProgress(false)

                if (e.statusCode == 404) {
                    // Show signup UI.
                    viewModel.foundEmail(Pair(email, false))

                    return@launch
                }

                enableInput(true)
                activity?.handleApiError(e)

                info(e)
            } catch (e: Exception) {

                viewModel.showProgress(false)
                enableInput(true)

                activity?.handleException(e)

                info(e)
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = EmailFragment()
    }
}