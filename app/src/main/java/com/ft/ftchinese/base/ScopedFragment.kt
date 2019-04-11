package com.ft.ftchinese.base

import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

@kotlinx.coroutines.ExperimentalCoroutinesApi
abstract class ScopedFragment: Fragment(),
        CoroutineScope by MainScope() {
    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}