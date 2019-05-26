package com.ft.ftchinese.ui.login

import android.os.Bundle
import com.ft.ftchinese.base.ScopedFragment
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignUpFragment : ScopedFragment(),
        AnkoLogger {

    companion object {

        private const val ARG_EMAIL = "arg_email"
        private const val ARG_HOST_TYPE = "arg_host_type"

        @JvmStatic
        fun newInstance(email: String, host: Int) = SignUpFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
                putInt(ARG_HOST_TYPE, host)
            }
        }
    }
}