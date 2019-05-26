package com.ft.ftchinese.ui.login

import android.os.Bundle
import com.ft.ftchinese.base.ScopedFragment
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class SignInFragment : ScopedFragment(),
        AnkoLogger {

    companion object {
        private const val ARG_EMAIL = "arg_email"

        @JvmStatic
        fun newInstance(email: String) = SignInFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_EMAIL, email)
            }
        }
    }
}