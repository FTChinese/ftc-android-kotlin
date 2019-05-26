package com.ft.ftchinese.ui.login

import com.ft.ftchinese.base.ScopedFragment
import org.jetbrains.anko.AnkoLogger

@kotlinx.coroutines.ExperimentalCoroutinesApi
class EmailFragment : ScopedFragment(),
        AnkoLogger {

    companion object {
        @JvmStatic
        fun newInstance() = EmailFragment()
    }
}